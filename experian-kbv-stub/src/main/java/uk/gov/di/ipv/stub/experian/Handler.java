package uk.gov.di.ipv.stub.experian;

import com.experian.uk.schema.experian.identityiq.services.webservice.AnswerFormat;
import com.experian.uk.schema.experian.identityiq.services.webservice.ArrayOfString;
import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.experian.uk.schema.experian.identityiq.services.webservice.Questions;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQRequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;
import com.experian.uk.schema.experian.identityiq.services.webservice.ResultsQuestions;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAARequest;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import com.experian.uk.wasp.LoginWithCertificateResponse;
import com.experian.uk.wasp.STSResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Handler {

    private static Logger LOGGER = LoggerFactory.getLogger(Handler.class);
    private Unmarshaller saaUnmarshaller;
    private Marshaller saaResponseMarshaller;
    private Unmarshaller rtqUnmarshaller;
    private Marshaller rtqResponseMarshaller;
    private Marshaller tokenResponseMarshaller;

    protected Handler() throws JAXBException {
        saaUnmarshaller = JAXBContext.newInstance(SAARequest.class).createUnmarshaller();
        saaResponseMarshaller = JAXBContext.newInstance(SAAResponse.class).createMarshaller();
        rtqUnmarshaller = JAXBContext.newInstance(RTQRequest.class).createUnmarshaller();
        rtqResponseMarshaller = JAXBContext.newInstance(RTQResponse.class).createMarshaller();
        tokenResponseMarshaller = JAXBContext.newInstance(LoginWithCertificateResponse.class).createMarshaller();
    }

    protected Route root = (Request request, Response response) -> "ok";

    protected Route tokenRequest =
            (Request request, Response response) -> {


                /**
                 *
                 * <?xml version="1.0" encoding="utf-8"?>
                 * <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                 *     <soap:Body>
                 *         <LoginWithCertificateResponse xmlns="http://www.uk.experian.com/WASP/">
                 *             <LoginWithCertificateResult>string</LoginWithCertificateResult>
                 *         </LoginWithCertificateResponse>
                 *     </soap:Body>
                 * </soap:Envelope>
                 */
                LoginWithCertificateResponse loginWithCertificateResponse = new LoginWithCertificateResponse();
                loginWithCertificateResponse.setLoginWithCertificateResult("stub-token-" + System.currentTimeMillis());



                StringWriter sw = new StringWriter();
                tokenResponseMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
                tokenResponseMarshaller.marshal(loginWithCertificateResponse, sw);
                response.header("Content-Type", "application/soap+xml");

                String body = sw.toString();

                String soapHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                        "<soap:Body>";

                String soapFooter = "</soap:Body></soap:Envelope>";

                String resp = soapHeader + body + soapFooter;
                LOGGER.info("STS soap response is:"  + resp);
                return resp;
            };

    protected Route responseToQuestions =
            (Request request, Response response) -> {
                String body = request.body();
                LOGGER.info("responseToQuestions body: " + body);
                StringReader reader = new StringReader(body);
                RTQRequest rtqRequest = (RTQRequest) rtqUnmarshaller.unmarshal(reader);
                Control control = rtqRequest.getControl();

                RTQResponse rtqResponse = new RTQResponse();
                RTQResponse2 result = new RTQResponse2();
                rtqResponse.setRTQResult(result);
                result.setControl(control);

                Results results = new Results();
                results.setOutcome("Authentication successful – capture SQ");
                results.setAuthenticationResult("Authenticated");
                List<String> nextTransIdList = results.getNextTransId().getString();
                nextTransIdList.add("END");
                ResultsQuestions questions = results.getQuestions();
                questions.setAsked(2);
                questions.setCorrect(2);
                questions.setIncorrect(0);
                result.setResults(results);

                StringWriter sw = new StringWriter();
                rtqResponseMarshaller.marshal(rtqResponse, sw);
                response.header("Content-Type", "application/soap+xml");
                return sw.toString();
            };

    protected Route startAuthenticationAttempt =
            (Request request, Response response) -> {
                String body = request.body();
                LOGGER.info("startAuthenticationAttempt body: " + body);
                StringReader reader = new StringReader(body);
                SAARequest saaRequest = (SAARequest) saaUnmarshaller.unmarshal(reader);
                Control control = saaRequest.getControl();
                control.setAuthRefNo(UUID.randomUUID().toString());
                if (control.getURN() == null) {
                    control.setURN(UUID.randomUUID().toString());
                }

                SAAResponse saaResponse = new SAAResponse();
                SAAResponse2 saaResult = new SAAResponse2();
                saaResponse.setSAAResult(saaResult);
                saaResult.setControl(control);
                Questions questions = new Questions();
                List<Question> questionList = questions.getQuestion();
                questionList.add(getQuestion1());
                questionList.add(getQuestion2());
                saaResult.setQuestions(questions);

                Results results = new Results();
                results.setOutcome("Authentication Questions returned");
                ArrayOfString nextTransId = new ArrayOfString();
                nextTransId.getString().add("RTQ");
                results.setNextTransId(nextTransId);
                saaResult.setResults(results);

                StringWriter sw = new StringWriter();
                saaResponseMarshaller.marshal(saaResponse, sw);
                response.header("Content-Type", "application/soap+xml");
                return sw.toString();
            };

    private Question getQuestion1() {
        Question question = new Question();
        question.setQuestionID("Q00001");
        question.setText("question 1");
        question.setTooltip("question 1 tooltip");
        AnswerFormat answerFormat = question.getAnswerFormat();
        List<String> answerList = answerFormat.getAnswerList();
        answerList.add("Correct");
        answerList.add("Incorrect");
        answerList.add("Error now");
        answerFormat.setFieldType("G");
        answerFormat.setIdentifier("A00004");
        return question;
    }

    private Question getQuestion2() {
        Question question = new Question();
        question.setQuestionID("Q00002");
        question.setText("question 2");
        question.setTooltip("question 2 tooltip");
        AnswerFormat answerFormat = question.getAnswerFormat();
        List<String> answerList = answerFormat.getAnswerList();
        answerList.add("Correct");
        answerList.add("Incorrect");
        answerList.add("Error now");
        answerFormat.setFieldType("G");
        answerFormat.setIdentifier("A00004");
        return question;
    }
}
