package edu.ryan.rest.assured.example;

import java.util.List;

import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;
import com.jayway.restassured.specification.RequestSpecification;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.json.JsonPath.from;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

public class MessagesIT {

    // http://stackoverflow.com/questions/17433046/integration-test-of-rest-apis-with-code-coverage
    // https://github.com/jayway/rest-assured/wiki/Usage#json-using-jsonpath
    // http://static.javadoc.io/com.jayway.restassured/json-path/2.8.0/com/jayway/restassured/path/json/JsonPath.html
    // https://github.com/jayway/rest-assured/blob/master/examples/rest-assured-itest-java/src/test/java/com/jayway/restassured/itest/java/CookieITest.java
    // https://github.com/jayway/rest-assured/blob/master/examples/rest-assured-itest-java/src/test/java/com/jayway/restassured/itest/java/RedirectITest.java
    // https://github.com/jayway/rest-assured/search?utf8=%E2%9C%93&q=ValidatableResponse&type=Code
    // http://james-willett.com/2015/06/extracting-a-json-response-with-rest-assured/

// get all, empty list
// post, data back with id
// get all, see 1 item in list
// get single with id, data back
// post updated data with id, updated data
// get single with id, updated data back
// put updated data with id, updated data back
// get single with id, updated data back
// delete id, get data back
// get all, empty list
// get single with old id, 404 not found


    private static final int PORT = 4567;
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "password123";

    private static final String SUBJECT_1 = "subject1";
    private static final String SUBJECT_2 = "subject2";
    private static final String SUBJECT_3 = "subject3";

    private static final String BODY_1 = "body1";
    private static final String BODY_2 = "body2";
    private static final String BODY_3 = "body3";

    private static String sessionId;

    @BeforeClass
    public static void setUp() {
        // login
        sessionId = given()
                .port(PORT)
                .redirects()
                .follow(false) // must turn off automatic following of redirects or else the cookies will be lost
                .contentType("application/x-www-form-urlencoded")
                .param("username", USERNAME)
                .param("password", PASSWORD)
                .when()
                .post("/login")
                .then()
//                .statusCode(302) // can't seem to control Sinatra to use 302 instead of 303, but it doesn't matter
                .extract()
                .cookie("sessionId");
    }

    @AfterClass
    public static void tearDown() {
        // logout
        given()
                .port(PORT)
                .redirects()
                .follow(false) // turn off auto follow redirects so won't close connection while serving up forwards
                .contentType("application/x-www-form-urlencoded")
                .cookie("sessionId", sessionId)
                .when()
                .post("/logout")
                .then();
//                .statusCode(302); // can't seem to control Sinatra to use 302 instead of 303, but it doesn't matter
    }

    private RequestSpecification givenCommon() {
        return given()
                .port(PORT)
                .cookie("sessionId", sessionId);
    }

    private RequestSpecification givenPostCommon() {
        return givenCommon()
                .contentType("application/json");
    }

    private int create(String subject, String body) {
        return givenPostCommon()
                .content(
                        String.format(
                                "{\"subject\": \"%s\", \"body\": \"%s\"}",
                                subject,
                                body
                        )
                )
                .when()
                .post("/messages")
                .then()
                .statusCode(200)
                .body("subject", equalTo(subject))
                .body("body", equalTo(body))
                .extract()
                .path("id");
    }

    private void update(int id, String subject, String body) {
        givenPostCommon()
                .content(
                        String.format(
                                "{\"id\": %d, \"subject\": \"%s\", \"body\": \"%s\"}",
                                id,
                                subject,
                                body
                        )
                )
                .when()
                .post(String.format("/messages/%d", id))
                .then()
                .statusCode(200)
                .body("subject", equalTo(subject))
                .body("body", equalTo(body))
                .body("id", equalTo(id));
    }

    private ValidatableResponse deleteCommon(int id) {
        return givenCommon()
                .when()
                .delete(String.format("/messages/%d", id))
                .then();
    }

    private void delete(int id, String subject, String body) {
        deleteCommon(id)
                .statusCode(200)
                .body("id", equalTo(id))
                .body("subject", equalTo(subject))
                .body("body", equalTo(body));
    }

    private void deleteSafe(int id) {
        try {
            deleteCommon(id);
        } catch (Exception e) {
            System.err.println("Burying exception to try to clean up all resources created!");
            e.printStackTrace();
        }
    }

    private ValidatableResponse getAllCommon() {
        return givenCommon()
                .when()
                .get("/messages")
                .then()
                .statusCode(200);
    }

    private int getAllSize() {
        return getAllCommon()
                .extract()
                .response()
                .body()
                .path("size()");
    }

    private Response verifyGetAll(int size) {
        return getAllCommon()
                .body("", hasSize(size))
                .extract()
                .response();
    }

    private void verifyGetAllNotContains(int size, int... ids) {
        Response response = verifyGetAll(size);
        List<Integer> idsToVerify = from(response.asString()).get("id");
        for (int id : ids) {
            assertFalse(idsToVerify.contains(id));
        }
    }

    private void verifyGetAllContains(int size, int... ids) {
        Response response = verifyGetAll(size);
        List<Integer> idsToVerify = from(response.asString()).get("id");
        for (int id : ids) {
            assertTrue(idsToVerify.contains(id));
        }
    }

    private ValidatableResponse getSingleCommon(int id) {
        return givenCommon()
                .when()
                .get(String.format("/messages/%d", id))
                .then();
    }

    private void verifyGetSingle(int id, String subject, String body) {
        getSingleCommon(id)
                .statusCode(200)
                .body("id", equalTo(id))
                .body("subject", equalTo(subject))
                .body("body", equalTo(body));
    }

    private void verifyNotFound(int id) {
        getSingleCommon(id)
                .statusCode(404);
    }

    @Test
    public void testCrud() throws Exception {

        int size = getAllSize();

        verifyGetAll(size);

        int id1 = create(SUBJECT_1, BODY_1);
        int id2 = create(SUBJECT_2, BODY_2);
        int id3 = create(SUBJECT_3, BODY_3);
 
        try {
            verifyGetAllContains(size + 3, id1, id2, id3);

            verifyGetSingle(id1, SUBJECT_1, BODY_1);
            verifyGetSingle(id2, SUBJECT_2, BODY_2);
            verifyGetSingle(id3, SUBJECT_3, BODY_3);

            update(id1, SUBJECT_1 + "-updated", BODY_1 + "-updated");
            update(id2, SUBJECT_2 + "-updated", BODY_2 + "-updated");
            update(id3, SUBJECT_3 + "-updated", BODY_3 + "-updated");

            verifyGetAllContains(size + 3, id1, id2, id3);

            verifyGetSingle(id1, SUBJECT_1 + "-updated", BODY_1 + "-updated");
            verifyGetSingle(id2, SUBJECT_2 + "-updated", BODY_2 + "-updated");
            verifyGetSingle(id3, SUBJECT_3 + "-updated", BODY_3 + "-updated");

            delete(id1, SUBJECT_1 + "-updated", BODY_1 + "-updated");
            delete(id2, SUBJECT_2 + "-updated", BODY_2 + "-updated");
            delete(id3, SUBJECT_3 + "-updated", BODY_3 + "-updated");

            verifyGetAllNotContains(size, id1, id2, id3);

            verifyNotFound(id1);
            verifyNotFound(id2);
            verifyNotFound(id3);

        } finally {
            deleteSafe(id1);
            deleteSafe(id2);
            deleteSafe(id3);
        }
    }

}
