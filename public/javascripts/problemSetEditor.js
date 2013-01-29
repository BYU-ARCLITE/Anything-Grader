
var problemSetId = window.location.pathname.substring(window.location.pathname.lastIndexOf("/") + 1);

function getProblemData(id) {
    return {
        problemSet: problemSetId,
        answers: JSON.stringify($("#answersList_"+id+" > li").children(".answer").map(function(){return $(this).text();}).get()),
        problemType: $("#problemType_" + id).val(),
        points: $("#points_" + id).val(),
        acceptanceRate: $("#acceptanceRate_" + id).val(),
        caseModifier: $("#caseModifier_" + id).is(":checked"),
        punctuationModifier: $("#punctuationModifier_" + id).is(":checked"),
        wordOrderModifier: $("#wordOrderModifier_" + id).is(":checked"),
        responseOrderModifier: $("#responseOrderModifier_" + id).is(":checked"),
        gradientGradeMethod: $("#gradientGradeMethod_" + id).is(":checked"),
        subtractiveModifier: $("#subtractiveModifier_" + id).is(":checked")
    };
}

function setupRemoveAnswers() {
    $(".removeAnswer").click(function() { $(this).parent().remove(); return false; });
}

$(function() {
    $(".saveBtn").click(function() {
        var problemSetData = {
            name: $("#name").val()
        };
        $.ajax("/problemSets/" + problemSetId, {
            type: "post",
            data: problemSetData,
            dataType: "json",
            success: function(data) {
                if(!data.success)
                    alert("Error: " + data.message);
            },
            error: function(data) {
                alert("Error: " + data.message);
            }
        })
    });

    $(".addAnswerButton").click(function() {
        // Get the answer text
        var id = $(this).attr("data-problem-id"),
            $textbox = $("#answers_" + id),
            answer = $textbox.val();
        $textbox.val("");

        // Add the answer to the list
        $("#answersList_" + id).append("<li><span class='answer'>"+answer+"</span>&nbsp;<a href='#' class='removeAnswer'>&times;</a></li>");
        setupRemoveAnswers();
    });
    setupRemoveAnswers();

    $(".createProblem").click(function() {
        $.ajax("/problems", {
            type: "post",
            data: getProblemData("new"),
            dataType: "json",
            success: function(data) {
                if(!data.success)
                    alert("Error: " + data.message);
                else
                    location.reload();
            },
            error: function(data) {
                alert("Error: " + data.message);
            }
        })
    });

    $(".saveProblem").click(function() {
        var id = $(this).attr("data-problem-id");
        $.ajax("/problems/" + id, {
            type: "post",
            data: getProblemData(id),
            dataType: "json",
            success: function(data) {
                if(!data.success)
                    alert("Error: " + data.message);
//                else
//                    location.reload();
            },
            error: function(data) {
                alert("Error: " + data.message);
            }
        })
    });

    $(".deleteProblem").click(function() {
        var id = $(this).attr("data-problem-id");
        $.ajax("/problems/" + id + "/delete", {
            success: function(data) {
                if(!data.success)
                    alert("Error: " + data.message);
                else
                    location.reload();
            },
            error: function(data) {
                //alert("Error: " + data.message);
                alert(JSON.stringify(data));
            }
        })
    });


});