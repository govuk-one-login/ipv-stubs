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
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.MimeHeaders;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import spark.Request;
import spark.Response;
import spark.Route;

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
    private static final String NO_FURTHER_QUESTIONS = "Insufficient additional questions";
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
                                "Correct 2",
                                "Incorrect 2",
                                USER_DATA_INCORRECT,
                                EXPERIAN_ERROR,
                                NO_FURTHER_QUESTIONS));
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
        Results results = new Results();
        ArrayOfString nextTransId = new ArrayOfString();
        saaResult.setControl(control);

        if (saaObject
                        .getSAARequest()
                        .getApplicant()
                        .getName()
                        .getForename()
                        .equalsIgnoreCase("SUZIE")
                && saaObject
                        .getSAARequest()
                        .getApplicant()
                        .getName()
                        .getSurname()
                        .equalsIgnoreCase("SHREEVE")) {
            simulateThinFileResult(sw, saaResponse, saaResult, results, nextTransId);
            return;
        }

        Questions questions = new Questions();
        List<Question> questionList = questions.getQuestion();
        questionList.add(getQuestion1());
        questionList.add(getQuestion2());
        saaResult.setQuestions(questions);
        results.setOutcome("Authentication Questions returned");
        nextTransId.getString().add("RTQ");
        results.setNextTransId(nextTransId);
        saaResult.setResults(results);

        saaResponse.setSAAResult(saaResult);

        saaResponseMarshaller.marshal(saaResponse, sw);
    }

    private void simulateThinFileResult(
            StringWriter sw,
            SAAResponse saaResponse,
            SAAResponse2 saaResult,
            Results results,
            ArrayOfString nextTransId)
            throws JAXBException {
        results.setOutcome("Insufficient Questions (Unable to Authenticate)");
        results.setAuthenticationResult(UNABLE_TO_AUTHENTICATE);
        nextTransId.getString().add("END");
        results.setNextTransId(nextTransId);
        saaResult.setResults(results);
        saaResponse.setSAAResult(saaResult);

        saaResponseMarshaller.marshal(saaResponse, sw);
        return;
    }

    private void simulateThinFileResult(
            StringWriter sw,
            RTQResponse response,
            RTQResponse2 result,
            Results results,
            ArrayOfString nextTransId)
            throws JAXBException {
        results.setOutcome("Insufficient Questions (Unable to Authenticate)");
        results.setAuthenticationResult(UNABLE_TO_AUTHENTICATE);
        nextTransId.getString().add("END");
        results.setNextTransId(nextTransId);
        result.setResults(results);
        ResultsQuestions resultsQuestions = new ResultsQuestions();
        resultsQuestions.setAsked(3);
        resultsQuestions.setCorrect(1);
        resultsQuestions.setIncorrect(2);
        results.setQuestions(resultsQuestions);
        response.setRTQResult(result);
        rtqResponseMarshaller.marshal(response, sw);
        return;
    }

    private void stubRTQ(StringWriter sw, Document bodyDoc) throws JAXBException {
        RTQ rtqRequest = (RTQ) rtqUnmarshaller.unmarshal(bodyDoc);
        RTQResponse rtqResponse = new RTQResponse();
        RTQResponse2 result = new RTQResponse2();
        Results results = new Results();
        ArrayOfString nextTransId = new ArrayOfString();
        nextTransId.getString().addAll(List.of("END"));
        results.setNextTransId(nextTransId);
        result.setControl(rtqRequest.getRTQRequest().getControl());

        if (hasAnswer(rtqRequest, EXPERIAN_ERROR)) {
            throwSOAPFaultException("A general SoapFault has occurred at the Experian IIQ Stub.");
        }

        if (hasAnswer(rtqRequest, USER_DATA_INCORRECT)) {
            Error error = new Error();
            error.setErrorCode("1024");
            error.setMessage(UNABLE_TO_AUTHENTICATE);
            result.setError(error);
            rtqResponse.setRTQResult(result);

            rtqResponseMarshaller.marshal(rtqResponse, sw);
            return;
        }

        if (hasAnswer(rtqRequest, NO_FURTHER_QUESTIONS)) {
            simulateThinFileResult(sw, rtqResponse, result, results, nextTransId);
            return;
        }

        // check if Correct Answer was chosen

        ResultsQuestions resultsQuestions = new ResultsQuestions();
        if (hasAnswer(rtqRequest, "Incorrect")) {
            results.setOutcome(AUTHENTICATION_UNSUCCESSFUL);
            results.setAuthenticationResult(NOT_AUTHENTICATED);
            resultsQuestions.setCorrect(1);
            resultsQuestions.setIncorrect(2); // required to trigger the contra-indicator V03
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

    private boolean hasAnswer(RTQ rtqRequest, String answerToTest) {
        return rtqRequest.getRTQRequest().getResponses().getResponse().stream()
                .anyMatch(item -> item.getAnswerGiven().startsWith(answerToTest));
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
