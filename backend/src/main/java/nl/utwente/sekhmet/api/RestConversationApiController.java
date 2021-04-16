package nl.utwente.sekhmet.api;


import com.google.gson.*;
import nl.utwente.sekhmet.auth.SupernaturalAuthUser;
import nl.utwente.sekhmet.TheSecurityEnforcer;
import nl.utwente.sekhmet.jpa.service.*;
import nl.utwente.sekhmet.jpa.model.*;
import nl.utwente.sekhmet.jpa.repositories.*;
import nl.utwente.sekhmet.jpa.model.Conversation;
import nl.utwente.sekhmet.jpa.model.Message;
import nl.utwente.sekhmet.jpa.repositories.ConversationRepository;
import nl.utwente.sekhmet.jpa.repositories.MessageRepository;
import nl.utwente.sekhmet.jpa.repositories.TestRepository;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

@RestController
@RequestMapping(value = "/api")
public class RestConversationApiController {
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    private static Gson gson = new GsonBuilder().serializeNulls().create();


    public RestConversationApiController(MessageRepository messageRepository, ConversationRepository conversationRepository, TestRepository testRepository, UserRepository userRepository, EnrollmentRepository enrollmentRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.testRepository = testRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    // Get Conversations
    // with params to determine the user_id
    @GetMapping(value = "tests/{testId}")
    public ResponseEntity<String> getAllConversation(@PathVariable("testId") Long testId,
                                                     @AuthenticationPrincipal SupernaturalAuthUser principal) {
        long uid = principal.getUser().getId();
        try {
            Test test = TestService.getTest(testId, testRepository);

            Enrollment enrollment = EnrollmentService.getEnrollment(testId, uid, enrollmentRepository);

            boolean coordinator = TheSecurityEnforcer.isCoordinatorForTest(uid,testId);
            boolean systemAdmin = userRepository.findUserById(uid).isSystemAdmin();
            boolean opened = !(test.getStartTime() == null || test.getEndTime() != null);

            char role ;
            if(enrollment == null && !coordinator && !systemAdmin) {
                return ResponseEntity.ok("{\"response\": \"Test_id: " + testId + ";Enrollment is null, not coordinator, and not systemAdmin\"}");
            }
            try {
                role = enrollment.getRole();
            } catch (NullPointerException e){
                if (!coordinator && !systemAdmin) {
                    //unenrolled student
                    return ResponseEntity.ok("{\"response\": \"Test_id: " + testId + ";Unenrolled Student\"}");
                }
                role = Enrollment.Role.TEACHER;
            }

            if (!opened && role == Enrollment.Role.STUDENT) {
                return new ResponseEntity<>(
                        "{\"response\": \"Test_id : " + testId + ";Test is not active\"}", HttpStatus.NOT_ACCEPTABLE);
            }

            List<Conversation> conversations = ConversationService.getConversationByTest(test, conversationRepository);

            List<Enrollment> enrolled = new ArrayList<>(test.getEnrolled());
            List<User> allPeople = new ArrayList<>();
            List<User> teachers = new ArrayList<>();
            List<Character> allPeopleRoles = new ArrayList<>();
            for (Enrollment e : enrolled) {
                if (e.getUser().getId().equals(uid)) {
                    continue;
                    //No need to add self to list
                } else {
                    allPeople.add(e.getUser());
                    allPeopleRoles.add(e.getRole());
                }
                if (e.getRole() == 'T')  {
                    teachers.add(e.getUser());
                }
            }
            //This people object contains all people enrolled in the given test
            JsonObject peopleJson = User.usersToJsonObject(allPeople,false,allPeopleRoles);
            JsonObject teacherListJson = User.usersToJsonObject(teachers,false,null);
            JsonObject selfJson = (JsonObject) User.userToJsonObject(principal.getUser(),false);
            selfJson.addProperty("is_supervisor",role=='T');

            JsonObject res = new JsonObject();

            res.addProperty("test_id",testId);
            res.addProperty("module_coordinator", test.getCourse().getModuleCoordinator().getId());
            res.addProperty("name",test.getName());
            res.addProperty("start_time",test.getStartTime());
            res.addProperty("end_time",test.getEndTime());
            res.addProperty("user_id",uid);
            JsonObject chatsJson = new JsonObject();

            //TODO replace by CurrentUser (faster)
            User user = UserService.getUser(uid, userRepository);
            boolean first = true;
            if (role == 'S'){
                for (Conversation con: conversations){
                    if (con.getUser1()==null || !(con.getUser1().equals(user))) { //check if the student is really a part of a chat else skip the chat
                        continue;
                    }
                    first = false;
                    //List<Message> messages = messageRepository.findMessagesByMessageId_ConversationIdOrderByMessageId_TimestampDesc(con.getId());
                    List<Message> messages = new ArrayList<>(con.getMessages());

                    JsonObject convo = Conversation.conversationToJsonElement(con,messages,teacherListJson,role,false);
                    convo.addProperty("name","Questions");
                    convo.add("people",teacherListJson);

                    chatsJson.add(con.getId().toString(),convo);

                }
            } else {
                for (Conversation con : conversations) {
                    User temp =  con.getUser1();
                    List<Message> messages = new ArrayList<>(con.getMessages()==null ? new HashSet<>(): con.getMessages());
                    if ( temp != null) {
                        if (!allPeople.contains(temp)) {
                            continue;
                        }
                        JsonObject actuallyTeacherPlusOneStudent = teacherListJson.deepCopy();
                        actuallyTeacherPlusOneStudent.add(temp.getId().toString(),User.userToJsonObject(temp,false));
                        JsonObject convo = Conversation.conversationToJsonElement(con,messages,actuallyTeacherPlusOneStudent,role,false);
                        convo.addProperty("name",temp.getName());

                        chatsJson.add(con.getId().toString(),convo);
                    }
                }
                if (test.getTeacherConversation()!= null) {
                    Conversation con = test.getTeacherConversation();
                    List<Message> messages = messageRepository.findMessagesByMessageId_ConversationIdOrderByMessageId_TimestampDesc(con.getId());

                    JsonObject convo = Conversation.conversationToJsonElement(con,messages,teacherListJson,role,false);
                    convo.addProperty("name","Teacher chat");
                    chatsJson.add(con.getId().toString(),convo);
                }
            }
            if (test.getAnnouncements() != null) {
                Conversation con = test.getAnnouncements();
                List<Message> messages = messageRepository.findMessagesByMessageId_ConversationIdOrderByMessageId_TimestampDesc(con.getId());

                JsonObject convo = Conversation.conversationToJsonElement(con,messages,peopleJson,role,true);
                convo.addProperty("name","Announcements");
                chatsJson.add(con.getId().toString(),convo);

            }
            if (enrollment != null) {
                peopleJson.add(user.getId().toString(),selfJson);
            }
            res.add("people",peopleJson);
            res.add("chats",chatsJson);
            res.addProperty("course_id", test.getCourse().getCanvasId());

//            String resAfter = StringChecker.escapeString(gson.toJson(res2));
//            JSONObject jo = new JSONObject(gson.toJson(resAfter);
            return ResponseEntity.ok(gson.toJson(res)); //close entire object
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(
                    "{\"response\": \""+e.getMessage()+"\"}", HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"" + e.getMessage() + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/tests/{id}/backlog", produces = "text/csv")
    public ResponseEntity<String> getBacklogExcel(@PathVariable("id") Long tid, HttpServletResponse response,
                                                  @AuthenticationPrincipal SupernaturalAuthUser principal) {
        long uid = principal.getUser().getId();
        if(!TheSecurityEnforcer.isCoordinatorForTest(uid,tid)){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        try {
            XSSFWorkbook workbook = new XSSFWorkbook();
            CellStyle titleStyle = workbook.createCellStyle();
            XSSFFont titleFont = workbook.createFont();
            titleFont.setFontHeight(13);
            titleFont.setBold(true);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle style = workbook.createCellStyle();
            XSSFFont font = workbook.createFont();
            font.setFontHeight(12);
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.LEFT);


            XSSFSheet sheetAN = workbook.createSheet("Announcement");
            XSSFSheet sheetTC = workbook.createSheet("Teacher Chat");
            XSSFSheet sheetMS = workbook.createSheet("Messages");

            int rowCount = 0;

            Test test = TestService.getTest(tid, testRepository);
            Date testDate = new Date(test.getStartTime());

            ////////////// ANNOUNCEMENTS //////////////////////
            Row row = sheetAN.createRow(rowCount++);
            createCell(sheetAN, row, 0, "TEST_ID:" + tid, titleStyle);
            createCell(sheetAN, row, 1, "Name: " + test.getName(), titleStyle);
            createCell(sheetAN, row, 2, "Date: " + testDate, titleStyle);

            sheetAN.createRow(rowCount++);


            List<Conversation> conList = ConversationService.getConversationByTest(test, conversationRepository);
            Conversation annCon = test.getAnnouncements();
            conList.remove(annCon);
            List<Message> annMesList = MessageService.getMessageByConversation(annCon, messageRepository);

            if (!annMesList.isEmpty()) {
                Row announce = sheetAN.createRow(rowCount++);
                createCell(sheetAN, announce, 0, "ANNOUNCEMENT", titleStyle);

                Row announceHeaders = sheetAN.createRow(rowCount++);
                createCell(sheetAN, announceHeaders, 0, "Message_id", titleStyle);
                createCell(sheetAN, announceHeaders, 1, "Timestamp", titleStyle);
                createCell(sheetAN, announceHeaders, 2, "Sender_id", titleStyle);
                createCell(sheetAN, announceHeaders, 3, "Sender_Name", titleStyle);
                createCell(sheetAN, announceHeaders, 4, "Content", titleStyle);

                Map<Long, List<Message>> mesMap = mesListToMap(annMesList);

                for (Map.Entry<Long, List<Message>> entry: mesMap.entrySet()) {
                    for (int m = 0; m < entry.getValue().size(); m++) {
                        Message mes = entry.getValue().get(m);

                        Date mesDate = new Date(mes.getTimestamp());
                        Row annRow = sheetAN.createRow(rowCount++);
                        int columnCount = 0;
                        String mesContent = mes.getContent();
                        if (entry.getValue().size() > 1) {
                            if (m == entry.getValue().size() - 1) {
                                mesContent += " (final edit)";
                            } else {
                                mesContent += " (edited)";
                            }
                        }
                        if (!mes.getVisible()) {
                            mesContent += " (removed)";
                        }
                        createCell(sheetAN, annRow, columnCount++, mes.getMid(), style);
                        createCell(sheetAN, annRow, columnCount++, mesDate.toString(), style);
                        createCell(sheetAN, annRow, columnCount++, mes.getSender().getId(), style);
                        createCell(sheetAN, annRow, columnCount++, mes.getSender().getName(), style);
                        createCell(sheetAN, annRow, columnCount++, mesContent, style);
                    }
                }
            } else {
                Row surprisingAnn = sheetAN.createRow(rowCount++);
                createCell(sheetAN, surprisingAnn, 0, "Surprisingly, no announcements (Not even a \"Good Luck!\")", titleStyle);
            }
            ////////////// TEACHER CHAT //////////////////////

            rowCount = 0;
            Row testIdRow = sheetTC.createRow(rowCount++);
            createCell(sheetTC, testIdRow, 0, "TEST_ID:" + tid, titleStyle);
            createCell(sheetTC, testIdRow, 1, "Name: " + test.getName(), titleStyle);
            createCell(sheetTC, testIdRow, 2, "Date: " + testDate, titleStyle);
            sheetTC.createRow(rowCount++);

            Conversation teachCon = test.getTeacherConversation();
            conList.remove(teachCon);
            List<Message> teachMesList = MessageService.getMessageByConversation(teachCon, messageRepository);

            if(!teachMesList.isEmpty()) {
                Row teacherChat = sheetTC.createRow(rowCount++);
                createCell(sheetTC, teacherChat, 0, "TEACHER CHAT", titleStyle);
                sheetTC.createRow(rowCount++);

                Row teacherChatHeaders = sheetTC.createRow(rowCount++);
                createCell(sheetTC, teacherChatHeaders, 0, "Message_id", titleStyle);
                createCell(sheetTC, teacherChatHeaders, 1, "Timestamp", titleStyle);
                createCell(sheetTC, teacherChatHeaders, 2, "Sender_id", titleStyle);
                createCell(sheetTC, teacherChatHeaders, 3, "Sender_Name", titleStyle);
                createCell(sheetTC, teacherChatHeaders, 4, "Content", titleStyle);

                Map<Long, List<Message>> mesMap = mesListToMap(teachMesList);

                for (Map.Entry<Long, List<Message>> entry: mesMap.entrySet()) {
                    for (int m = 0; m < entry.getValue().size(); m++) {
                        Message mes = entry.getValue().get(m);

                        Date mesDate = new Date(mes.getTimestamp());
                        Row annRow = sheetTC.createRow(rowCount++);
                        int columnCount = 0;
                        String mesContent = mes.getContent();
                        if (entry.getValue().size() > 1) {
                            if (m == entry.getValue().size() - 1) {
                                mesContent += " (final edit)";
                            } else {
                                mesContent += " (edited)";
                            }
                        }
                        if (!mes.getVisible()) {
                            mesContent += " (removed)";
                        }
                        createCell(sheetTC, annRow, columnCount++, mes.getMid(), style);
                        createCell(sheetTC, annRow, columnCount++, mesDate.toString(), style);
                        createCell(sheetTC, annRow, columnCount++, mes.getSender().getId(), style);
                        createCell(sheetTC, annRow, columnCount++, mes.getSender().getName(), style);
                        createCell(sheetTC, annRow, columnCount++, mesContent, style);
                    }
                }
            } else {
                Row surprisingTeach = sheetTC.createRow(rowCount++);
                createCell(sheetTC, surprisingTeach, 0, "Surprisingly, the teachers did not gossip about the students", titleStyle);
            }

            ////////////// STUDENT-TEACHER MESSAGE //////////////////////

            rowCount = 0;

            Row testIdRow1 = sheetMS.createRow(rowCount++);
            createCell(sheetMS, testIdRow1, 0, "TEST_ID: " + tid, titleStyle);
            createCell(sheetMS, testIdRow1, 1, "Name: " + test.getName(), titleStyle);
            createCell(sheetTC, testIdRow1, 2, "Date: " + testDate, titleStyle);
            sheetMS.createRow(rowCount++);


            if (!conList.isEmpty()){
                Row messageRow = sheetMS.createRow(rowCount++);
                createCell(sheetMS, messageRow, 0, "MESSAGES", titleStyle);
                sheetMS.createRow(rowCount++);


                for (Conversation con : conList) {
                    List<Message> tempMesList = MessageService.getMessageByConversation(con, messageRepository);
                    if (tempMesList.isEmpty()) {
                        continue;
                    }
                    Row chatId = sheetMS.createRow(rowCount++);
                    createCell(sheetMS, chatId, 0, "Chat_id: " + con.getId(), titleStyle);
                    Row studName = sheetMS.createRow(rowCount++);
                    createCell(sheetMS, studName, 0, "Student Name: " + con.getUser1().getName(), titleStyle);

                    Row messageHeaders = sheetMS.createRow(rowCount++);
                    createCell(sheetMS, messageHeaders, 0, "Message_id", titleStyle);
                    createCell(sheetMS, messageHeaders, 1, "Timestamp", titleStyle);
                    createCell(sheetMS, messageHeaders, 2, "Sender_id", titleStyle);
                    createCell(sheetMS, messageHeaders, 3, "Sender_Name", titleStyle);
                    createCell(sheetMS, messageHeaders, 4, "Content", titleStyle);

                    Map<Long, List<Message>> mesMap = mesListToMap(tempMesList);

                    for (Map.Entry<Long, List<Message>> entry: mesMap.entrySet()) {
                        for (int m = 0; m < entry.getValue().size(); m++) {
                            Message mes = entry.getValue().get(m);

                            Date mesDate = new Date(mes.getTimestamp());
                            Row annRow = sheetMS.createRow(rowCount++);
                            int columnCount = 0;
                            String mesContent = mes.getContent();
                            if (entry.getValue().size() > 1) {
                                if (m == entry.getValue().size() - 1) {
                                    mesContent += " (final edit)";
                                } else {
                                    mesContent += " (edited)";
                                }
                            }
                            if (!mes.getVisible()) {
                                mesContent += " (removed)";
                            }
                            createCell(sheetMS, annRow, columnCount++, mes.getMid(), style);
                            createCell(sheetMS, annRow, columnCount++, mesDate.toString(), style);
                            createCell(sheetMS, annRow, columnCount++, mes.getSender().getId(), style);
                            createCell(sheetMS, annRow, columnCount++, mes.getSender().getName(), style);
                            createCell(sheetMS, annRow, columnCount++, mesContent, style);
                        }
                    }
                    sheetMS.createRow(rowCount++);
                }
            } else {
                Row surprising = sheetMS.createRow(rowCount++);
                createCell(sheetMS, surprising, 0, "Surprisingly, no student asked a single question", titleStyle);
            }

            response.setContentType("application/octet-stream");
            String headerKey = "Content-Disposition";
            String headerValue = "attachment; filename=ChatBacklog_Test" + tid + ".xlsx";
            response.setHeader(headerKey, headerValue);
            ServletOutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();

            return ResponseEntity.ok("Exported to Excel");
        } catch (IOException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"IO Exception" + e.getMessage() + "\"}" , HttpStatus.NOT_ACCEPTABLE);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"" + e.getMessage() + "\"}", HttpStatus.NOT_ACCEPTABLE);
        }
    }


    @DeleteMapping(value = "tests/{tid}/backlog")
    public ResponseEntity<String> deleteBacklog(@PathVariable("tid") Long tid,
                                                @AuthenticationPrincipal SupernaturalAuthUser principal) {
        long uid = principal.getUser().getId();
        if(!TheSecurityEnforcer.isCoordinatorForTest(uid,tid)){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        try {
            Test test = TestService.getTest(tid, testRepository);
            if (!(test.getStartTime() == null || test.getEndTime() != null)) {
                return new ResponseEntity<>("" +
                        "{\"response\": \"Test is still active\"}", HttpStatus.NOT_ACCEPTABLE);
            }

            // DElETE MESSAGES
            List<Message> mesList = MessageService.getMessageByTest(test, messageRepository);
            if (!mesList.isEmpty()) {
                messageRepository.deleteAll(mesList);
            }

            // DELETE CON
            List<Conversation> conList = ConversationService.getConversationByTest(test, conversationRepository);
            if(!conList.isEmpty()){
                conversationRepository.deleteAll(conList);
            }

            test.setAnnouncements(null);
            test.setTeacherConversation(null);
            testRepository.save(test);

            return ResponseEntity.ok("{\"response\": \"Backlog Deleted\"}");
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"Element Not Found: " + e.getMessage() + "\"}", HttpStatus.NOT_FOUND);
        }
    }

    private void createCell(XSSFSheet sheet, Row row, int columnCount, Object value, CellStyle style) {
        sheet.autoSizeColumn(columnCount);
        Cell cell = row.createCell(columnCount);
        if (value instanceof Long) {
            cell.setCellValue((Long) value);
        }else {
            cell.setCellValue((String) value);
        }
        cell.setCellStyle(style);
    }

    private Map<Long, List<Message>> mesListToMap(List<Message> mesList) {
        Map<Long, List<Message>> resMap = new HashMap<>();
        if (mesList.isEmpty()){
            return resMap;
        }
        for (Message mes: mesList) {
            if (!resMap.containsKey(mes.getMid())) {
                List<Message> temp = new ArrayList<>();
                temp.add(mes);
                resMap.put(mes.getMid(), temp);
            } else {
                List<Message> temp = resMap.get(mes.getMid());
                temp.add(mes);
            }
        }
        return resMap;
    }
}
