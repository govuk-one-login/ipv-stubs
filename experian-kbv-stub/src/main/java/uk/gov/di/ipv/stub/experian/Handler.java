package uk.gov.di.ipv.stub.experian;

import com.experian.uk.schema.experian.identityiq.services.webservice.AnswerFormat;
import com.experian.uk.schema.experian.identityiq.services.webservice.ArrayOfString;
import com.experian.uk.schema.experian.identityiq.services.webservice.Control;
import com.experian.uk.schema.experian.identityiq.services.webservice.Error;
import com.experian.uk.schema.experian.identityiq.services.webservice.Question;
import com.experian.uk.schema.experian.identityiq.services.webservice.Questions;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQ;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse;
import com.experian.uk.schema.experian.identityiq.services.webservice.RTQResponse2;
import com.experian.uk.schema.experian.identityiq.services.webservice.Results;
import com.experian.uk.schema.experian.identityiq.services.webservice.ResultsQuestions;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAA;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse;
import com.experian.uk.schema.experian.identityiq.services.webservice.SAAResponse2;
import com.experian.uk.wasp.LoginWithCertificateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.soap.SOAPFaultException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Handler.class);
    private static final String AUTHENTICATION_UNSUCCESSFUL = "Authentication Unsuccessful";
    private static final String AUTHENTICATION_SUCCESSFUL =
            "Authentication successful – capture SQ";
    private static final String AUTHENTICATED = "Authenticated";
    private static final String NOT_AUTHENTICATED = "Not Authenticated";
    private static final String UNABLE_TO_AUTHENTICATE = "Unable to Authenticate";
    private static final String USER_DATA_INCORRECT = "User data incorrect";
    private static final String EXPERIAN_ERROR = "Experian SOAP Fault";
    private final String soapHeader =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                    + "<soap:Body>";
    private final String soapFooter = "</soap:Body></soap:Envelope>";

    private final Unmarshaller saaUnmarshaller;
    private final Marshaller saaResponseMarshaller;
    private final Unmarshaller rtqUnmarshaller;
    private final Marshaller rtqResponseMarshaller;
    private final Marshaller tokenResponseMarshaller;

    protected Handler() throws JAXBException {
        saaUnmarshaller = JAXBContext.newInstance(SAA.class).createUnmarshaller();
        saaResponseMarshaller = JAXBContext.newInstance(SAAResponse.class).createMarshaller();
        saaResponseMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        rtqUnmarshaller = JAXBContext.newInstance(RTQ.class).createUnmarshaller();
        rtqResponseMarshaller = JAXBContext.newInstance(RTQResponse.class).createMarshaller();
        rtqResponseMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        tokenResponseMarshaller =
                JAXBContext.newInstance(LoginWithCertificateResponse.class).createMarshaller();
        tokenResponseMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
    }

    protected Route root = (Request request, Response response) -> "ok";

    protected Route tokenRequest =
            (Request request, Response response) -> {
                LOGGER.info("tokenRequest body: " + request.body());
                /**
                 * <?xml version="1.0" encoding="utf-8"?> <soap:Envelope
                 * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 * xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                 * xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"> <soap:Body>
                 * <LoginWithCertificateResponse xmlns="http://www.uk.experian.com/WASP/">
                 * <LoginWithCertificateResult>string</LoginWithCertificateResult>
                 * </LoginWithCertificateResponse> </soap:Body> </soap:Envelope>
                 */
                LoginWithCertificateResponse loginWithCertificateResponse =
                        new LoginWithCertificateResponse();
                loginWithCertificateResponse.setLoginWithCertificateResult(
                        "stub-token-" + System.currentTimeMillis());

                String body = marshallToken(loginWithCertificateResponse);
                response.header("Content-Type", "application/soap+xml");

                String resp = soapHeader + body + soapFooter;
                LOGGER.info("tokenRequest response is:" + resp);
                return resp;
            };

    private String marshallToken(LoginWithCertificateResponse loginWithCertificateResponse)
            throws JAXBException, IOException {
        try (StringWriter sw = new StringWriter()) {
            tokenResponseMarshaller.marshal(loginWithCertificateResponse, sw);
            return sw.toString();
        }
    }

    private Question getQuestion1() {
        Question question = new Question();
        question.setQuestionID("Q00001");
        question.setText("Question 1");
        question.setTooltip("Question 1 Tooltip");
        AnswerFormat answerFormat = new AnswerFormat();
        answerFormat.setIdentifier("A00004");
        answerFormat.setFieldType("G");
        answerFormat.getAnswerList().addAll(Arrays.asList("Correct 1", "Incorrect 1"));
        question.setAnswerFormat(answerFormat);
        return question;
    }

    private Question getQuestion2() {
        Question question = new Question();
        question.setQuestionID("Q00002");
        question.setText("Question 2");
        question.setTooltip("Question 2 Tooltip");
        AnswerFormat answerFormat = new AnswerFormat();
        answerFormat
                .getAnswerList()
                .addAll(
                        Arrays.asList(
                                "Correct 2", "Incorrect 2", USER_DATA_INCORRECT, EXPERIAN_ERROR));
        answerFormat.setFieldType("G");
        answerFormat.setIdentifier("A00007");
        question.setAnswerFormat(answerFormat);
        return question;
    }

    protected Route iiqWebService =
            (Request request, Response response) -> {
                LOGGER.info("iiqWebService body: " + request.body());
                StringWriter sw = new StringWriter();
                String body = request.body();
                MessageFactory messageFactory = MessageFactory.newInstance();
                ByteArrayInputStream soapStringStream =
                        new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
                SOAPMessage soapMessage =
                        messageFactory.createMessage(new MimeHeaders(), soapStringStream);
                Document bodyDoc = soapMessage.getSOAPBody().extractContentAsDocument();

                NodeList saa = bodyDoc.getElementsByTagName("SAA");
                Node itemSAA = saa.item(0);
                if (itemSAA != null) {
                    stubSAA(sw, bodyDoc);
                }

                NodeList rtq = bodyDoc.getElementsByTagName("RTQ");
                Node itemRTQ = rtq.item(0);
                if (itemRTQ != null) {
                    stubRTQ(sw, bodyDoc);
                }
                response.header("Content-Type", "application/soap+xml");
                String resp = soapHeader + sw + soapFooter;
                LOGGER.info("iiqWebService response: " + resp);
                return resp;
            };

    private void stubSAA(StringWriter sw, Document bodyDoc) throws JAXBException {
        SAA saaObject = (SAA) saaUnmarshaller.unmarshal(bodyDoc);
        Control control = saaObject.getSAARequest().getControl();
        control.setAuthRefNo(UUID.randomUUID().toString());
        if (control.getURN() == null) {
            control.setURN(UUID.randomUUID().toString());
        }

        SAAResponse saaResponse = new SAAResponse();
        SAAResponse2 saaResult = new SAAResponse2();

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

        saaResponse.setSAAResult(saaResult);

        saaResponseMarshaller.marshal(saaResponse, sw);
    }

    private void stubRTQ(StringWriter sw, Document bodyDoc) throws JAXBException {
        RTQ rtqRequest = (RTQ) rtqUnmarshaller.unmarshal(bodyDoc);
        RTQResponse rtqResponse = new RTQResponse();
        RTQResponse2 result = new RTQResponse2();
        Results results = new Results();
        ArrayOfString arrayOfString = new ArrayOfString();
        arrayOfString.getString().addAll(List.of("END"));
        results.setNextTransId(arrayOfString);
        result.setControl(rtqRequest.getRTQRequest().getControl());

        boolean simulateExperianError =
                rtqRequest.getRTQRequest().getResponses().getResponse().stream()
                        .anyMatch(item -> item.getAnswerGiven().startsWith(EXPERIAN_ERROR));
        if (simulateExperianError) {
            throwSOAPFaultException("A general SoapFault has occurred at the Experian IIQ Stub.");
        }

        boolean simulateUserDataIncorrectError =
                rtqRequest.getRTQRequest().getResponses().getResponse().stream()
                        .anyMatch(item -> item.getAnswerGiven().startsWith(USER_DATA_INCORRECT));
        if (simulateUserDataIncorrectError) {
            Error error = new Error();
            error.setErrorCode("1024");
            error.setMessage(UNABLE_TO_AUTHENTICATE);
            result.setError(error);
            rtqResponse.setRTQResult(result);

            rtqResponseMarshaller.marshal(rtqResponse, sw);
            return;
        }

        // check if Correct Answer was chosen
        boolean correctAnswerNotSelected =
                rtqRequest.getRTQRequest().getResponses().getResponse().stream()
                        .anyMatch(item -> item.getAnswerGiven().startsWith("Incorrect"));

        ResultsQuestions resultsQuestions = new ResultsQuestions();
        if (correctAnswerNotSelected) {
            results.setOutcome(AUTHENTICATION_UNSUCCESSFUL);
            results.setAuthenticationResult(NOT_AUTHENTICATED);
            resultsQuestions.setCorrect(1);
            resultsQuestions.setIncorrect(1);
        } else {
            results.setOutcome(AUTHENTICATION_SUCCESSFUL);
            results.setAuthenticationResult(AUTHENTICATED);
            resultsQuestions.setCorrect(2);
            resultsQuestions.setIncorrect(0);
        }
        resultsQuestions.setAsked(2);
        results.setQuestions(resultsQuestions);
        result.setResults(results);

        rtqResponse.setRTQResult(result);

        rtqResponseMarshaller.marshal(rtqResponse, sw);
    }

    private void throwSOAPFaultException(String faultString) {
        SOAPFault soapFault;
        try {
            SOAPFactory soapFactory = SOAPFactory.newInstance();
            soapFault = soapFactory.createFault();
            soapFault.setFaultString(faultString);
        } catch (SOAPException e) {
            throw new RuntimeException("SOAP error");
        }
        throw new SOAPFaultException(soapFault);
    }
}
