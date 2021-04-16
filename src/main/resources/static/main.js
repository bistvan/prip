$(function() {
    $('#heaven').click(function() {window.location = '/workspace';});
    $('#ctrl #new-task').click(function() {
        var c = $('#current_tasks');
        var t = $('<div class="col-md-3"/>').text('task');
        c.append(t);
    });
});