package nl.utwente.sekhmet.canvas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;

public class CanvasApiClient {
    private static String baseUrl = "https://utwente-dev.instructure.com/api/";
//    private static String baseUrl = "https://canvas.utwente.nl/api/";
    private WebClient webClient = null;

    public CanvasApiClient(String accessToken) {
        this.webClient = WebClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
//                .defaultUriVariables(Collections.singletonMap("url", "http://localhost:8080"))
                .build();
    }

    public JsonElement doRestRequest(HttpMethod method, String uri) {
        return this.doRestRequest(method, uri, null);
    }

    public JsonElement doRestRequest(HttpMethod method, String uri, String body) {
        WebClient.RequestBodySpec x = webClient.method(method).uri(uri);
        if(method == HttpMethod.POST) {
            x.bodyValue(body);
            x.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }
        String result = x.retrieve().bodyToMono(String.class).block();

        return JsonParser.parseString(result);
    }

    private JsonElement doGraphQLRequest(String query) {
        JsonObject body = new JsonObject();
        body.addProperty("query", query);

//        System.out.println(body.toString());
        return this.doRestRequest(HttpMethod.POST, "graphql", body.toString());
    }

    public JsonArray getCoursesAvailableToImport() {
        JsonArray coursesArray = this.doRestRequest(
                HttpMethod.GET,
                "v1/courses?enrollment_role=TeacherEnrollment&per_page=100"
        ).getAsJsonArray();

        ArrayList<String> courseCodes = new ArrayList<>();
        for (JsonElement c : coursesArray) {
            JsonObject course = c.getAsJsonObject();
            courseCodes.add(course.get("course_code").getAsString());
        }

        return coursesArray;
    }

    // TODO: catch exceptions
    // TODO: what if canvas result is null? (empty)
    public JsonElement getAssignmentsAndGroups(int canvasCourseId) {
        JsonObject result = this.doGraphQLRequest(
                "query {" +
                        "  course(id: " + canvasCourseId + ") {" +
                        "    courseCode id _id" +
                        "    assignmentsConnection { nodes { name groupSet { _id id } } }" +
                        "    groupSetsConnection { nodes {name _id } }" +
                        "  }" +
                        "}"
        ).getAsJsonObject().get("data").getAsJsonObject().get("course").getAsJsonObject();

        JsonArray assignments = result.get("assignmentsConnection").getAsJsonObject().get("nodes").getAsJsonArray();
        JsonArray assignmentNames = new JsonArray();
        for (JsonElement c : assignments) {
            assignmentNames.add(c.getAsJsonObject().get("name").getAsString());
        }

        JsonArray groupSets = result.get("groupSetsConnection").getAsJsonObject().get("nodes").getAsJsonArray();
        JsonArray groupSetNamesAndIds = new JsonArray();
        for (JsonElement c : groupSets) {
            JsonObject groupSet = new JsonObject();
            groupSet.addProperty("canvasId", c.getAsJsonObject().get("_id").getAsString());
            groupSet.addProperty("name", c.getAsJsonObject().get("name").getAsString());
            groupSetNamesAndIds.add(groupSet);
        }

        JsonObject returnObject = new JsonObject();
        returnObject.add("assignments", assignmentNames);
        returnObject.add("groupsets", groupSetNamesAndIds);
        return returnObject;
    }

    // TODO: catch exceptions
    public JsonElement getFullCourseInformation(int canvasCourseId) {
        JsonObject result = this.doGraphQLRequest(
                "query MyQuery {" +
                        "  course(id: " + canvasCourseId + ") { courseCode name id _id" +
                        "    assignmentsConnection { nodes { name } }" +
                        "    enrollmentsConnection { nodes { type state user { name id sisId } } }" +
                        "    groupSetsConnection { nodes { name _id groupsConnection { nodes { membersConnection { edges { node { user { sisId } } } } } } } }\n" +
                        "  }" +
                        "}"
        ).getAsJsonObject().get("data").getAsJsonObject().get("course").getAsJsonObject();

        JsonObject course = new JsonObject();
        course.addProperty("id", result.get("_id").getAsString());
        course.addProperty("name", result.get("name").getAsString());
        course.addProperty("code", result.get("courseCode").getAsString());

        JsonObject teachers = new JsonObject();
        JsonObject students = new JsonObject();

        for (JsonElement e : result.get("enrollmentsConnection").getAsJsonObject().get("nodes").getAsJsonArray()) {
            JsonObject enrollment = e.getAsJsonObject();
            if(!enrollment.get("state").getAsString().equals("active") || enrollment.get("user").getAsJsonObject().get("sisId").isJsonNull()) {
                continue;
            }
            String enrollmentType = enrollment.get("type").getAsString();
            String sisId = enrollment.get("user").getAsJsonObject().get("sisId").getAsString();
            JsonElement user = enrollment.get("user").getAsJsonObject();
            if(enrollmentType.equals("StudentEnrollment")) {
                // add to students array
                // remove from teachers if it's already listed there
                if(teachers.has(sisId)) {
                    teachers.remove(sisId);
                }
                students.add(sisId, user);
            } else {
                // add to teachers array
                // only add to teachers if not yet in students
                if(!students.has(sisId)) {
                    teachers.add(sisId, user);
                }
            }
        }

        course.add("teachers", teachers);
        course.add("students", students);

        JsonObject groups = new JsonObject();
        for (JsonElement g : result.get("groupSetsConnection").getAsJsonObject().get("nodes").getAsJsonArray()) {
            JsonObject canvasGroup = g.getAsJsonObject();
            JsonArray group = new JsonArray();
            String canvasGroupId = canvasGroup.get("_id").getAsString();
            JsonArray groupConnection = canvasGroup.get("groupsConnection").getAsJsonObject().get("nodes").getAsJsonArray();
            if(groupConnection.size() < 1) {
                continue;
            }
            for (JsonElement gc : groupConnection) {
                JsonArray edges = gc.getAsJsonObject().get("membersConnection").getAsJsonObject().get("edges").getAsJsonArray();
                if(edges.size() < 1) {
                    continue;
                }
                for(JsonElement edge : edges) {
                    String sisId = edge.getAsJsonObject().get("node").getAsJsonObject().get("user").getAsJsonObject().get("sisId").getAsString();
                    if(!groups.has(canvasGroupId)) {
                        groups.add(canvasGroupId, new JsonArray());
                    }
                    groups.get(canvasGroupId).getAsJsonArray().add(sisId);
                }
            }
        }

        course.add("groups", groups);
        return course;
    }
}