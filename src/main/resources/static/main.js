$(function() {
    var ws = {tasks: []};
    var chg = false;
    var newTask = function(d, pre) {
        var t = $('<div class="col col-md-3"/>');
        var card = $('<div class="card shadow-sm c-task-box"/>').appendTo(t);
        var name = $('<input type=text/>').val(d.name).appendTo(card);

        $('#current_tasks')[pre ? 'prepend' : 'append'](t);

        if (typeof d.id == 'undefined') {
            if (pre)
                ws.tasks.unshift(d);
            else
                ws.tasks.push(d);
            changed(true);
        }
    };
    var showTasks = function(ws) {
        var c = $('#current_tasks').empty();
        for (var i = 0; i < ws.tasks.length; i++) {
            newTask(ws.tasks[i]);
        }
    }

    var guiInit = function() {
        $('#heaven').click(function() {window.location = '/workspace';});
        $('#ctrl #new-task').click(function() { newTask({name: "New Task"}, true)});
        $('.fn-save-ws a').click(function() { saveWorkspace(); })
    }

    var loadWorkspace = function() {
        $.getJSON("/workspace/data", function(d) {
            ws = d;
            showTasks(ws);
        });
    }

    var saveWorkspace = function() {
        changed(false);
    }

    var changed = function(ch) { // something has changed
        chg = ch;
        $('.fn-save-ws').css('display', ch ? '' : 'none');
    }

    guiInit();
    loadWorkspace();
});