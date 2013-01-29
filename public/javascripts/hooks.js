var ltiTemplate = '<imsx_POXEnvelopeRequest xmlns = "http://www.imsglobal.org/services/ltiv1p1/xsd/imsoms_v1p0"> <imsx_POXHeader> <imsx_POXRequestHeaderInfo> <imsx_version>V1.0</imsx_version> <imsx_messageIdentifier>{{random}}</imsx_messageIdentifier> </imsx_POXRequestHeaderInfo> </imsx_POXHeader> <imsx_POXBody> <replaceResultRequest> <resultRecord> <sourcedGUID> <sourcedId>{{sourcedId}}</sourcedId> </sourcedGUID> <result> <resultScore> <language>en</language> <textString>{{grade}}</textString> </resultScore> </result> </resultRecord> </replaceResultRequest> </imsx_POXBody> </imsx_POXEnvelopeRequest>';
$(function() {
    $("#useLti").click(function() {
        $("#additionalData").val(ltiTemplate);
        $("#contentType").val("application/xml");
        return false;
    });
});
