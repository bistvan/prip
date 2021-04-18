$(function() {
    var ws = {tasks: []};
    var chg = false;

    var def = function(o) {
        return typeof o != 'undefined' && o != null;
    }
    var set = function(prop, val, o) {
        if (!def(o))
            o = ws;
        var p = prop.split('.');
        var len = p.length - 1;
        for (var i = 0; i < len; i++) {
            var x = o[p[i]];
            if (!def(x))
                o[p[i]] = x = {};
            o = x;
        }
        var old = o[p[len]];
        if (def(old) ? !def(val) || old != val : def(val)) {
            changed(true);
        }
        o[p[len]] = val;
    }
    var parseDate = function(d) {
        return def(d) ? new Date(d) : null;
    }
    var now = function() {
        return new Date(Date.now());
    }
    var curDay = function() {
        return def(ws.current) ? ws.current : now();
    }
    var curWeek = function() {
        return addDay(curDay(), -curDay().getDay());
    }
    var sameDay = function(d1, d2) {
        return Math.trunc(d1.getTime() / 86400000) == Math.trunc(d2.getTime() / 86400000);
    }
    var addDay = function(d, diff) {
        return new Date(d.getTime() + (diff * 86400000));
    }
    var today = function(d) {
        return sameDay(d, now());
    }
    function pad(num, size) {
        if (!def(size))
            size = 2;
        num = num.toString();
        while (num.length < size) num = "0" + num;
        return num;
    }
    var timeStr = function(sec) {
        if (sec < 60)
            return sec + 's';
        if (sec < 3600)
            return pad(Math.trunc(sec / 60)) + ':' + pad(Math.trunc(sec % 60));
        return pad(Math.trunc(sec / 3600)) + ':' + pad(Math.trunc(sec % 3600 / 60));
    }
    var dateStr = function(d, x) {
        var t = d.getTime() - (d.getTimezoneOffset() * 60000);
        return new Date(t).toISOString().slice(0, def(x) ? x : 10);
    }

    var newTask = function(d, pre) {
        var t = $('<div class="col col-md-3"/>')[pre ? 'prependTo' : 'appendTo']('#current_tasks');
        var card = $('<div class="card shadow-sm c-task-box"/>').appendTo(t);
        var name = $('<input type=text class="form-control" placeholder="Task name *"/>').val(d.name).appendTo(card)
            .change(function() {
                set('name', $(this).val(), d);
                if (!def(d.id)) {
                    $.getJSON("/workspace/task?name=" + $(this).val(), function(s) {
                        if (s != null) {
                            name.val(d.name = s.name);
                            descr.val(d.descr = s.descr);
                            est.val(d.estimate = s.estimate);
                            id.text('#' + (d.id = s.id));
                        }
                    });
                }
            });
        var descr = $('<input type=text class="form-control form-control-sm"  placeholder="Task description"/>').val(d.descr).appendTo(card)
            .change(function() { set('descr', $(this).val(), d) });
        var est = $('<input type=text class="form-control form-control-sm"  placeholder="Original estimate"/>').val(d.estimate).appendTo(card)
            .change(function() { set('estimate', $(this).val(), d) });
        var text = $('<input type=text class="form-control form-control-sm"  placeholder="Activity description"/>').appendTo(card);
        var ctrl = $('<div class="task-ctrl"/>').appendTo(card);
        var id = $('<span/>').text(def(d.id) ? '#' + d.id : '#NONE').appendTo(ctrl);
        var logTime = $('<a href=# title="Log with time" class="log-with-time"/>').text('LogT').appendTo($('<span>').appendTo(ctrl));
        var log = $('<a href=# title="Log without time" class="log"/>').text('Log').appendTo($('<span>').appendTo(ctrl));
        var unpin = $('<a href=# title="Unpin this task" class="unpin"/>').text('Unpin').appendTo($('<span>').appendTo(ctrl))
            .click(function() {
                if (def(d.id)) {
                    set("pinned", false, d);
                }
                else {
                    for (var i = 0; i < ws.tasks.length;i++)
                        if (ws.tasks[i] == d) {
                            delete ws.tasks[i];
                            break;
                        }
                }
                t.remove();
            });

        if (!def(d.id)) {
            if (pre)
                ws.tasks.unshift(d);
            else
                ws.tasks.push(d);
            changed(true);
        }
    };

    var showTasks = function() {
        $('#current_tasks, #report-sum').empty();
        for (var i = 0; i < ws.tasks.length; i++) {
            newTask(ws.tasks[i]);
        }

        var start = curWeek();
        for (var i = 1; i < 7; i++) {
            var date = addDay(start, i);
            var t = $('<div class="col col-md-2"/>').appendTo('#report-sum');
            if (sameDay(date, curDay()))
                t.addClass('current-day');
            var cd = dateStr(date);
            var card = $('<div class="card shadow-sm c-report-sum"/>').appendTo(t);
            var a = $('<a href=#/>').text(cd).attr('data-cd', cd).data('date', date).appendTo($('<div/>').appendTo(card))
                .click(function() { loadWorkspace( $(this).data('date') ); });
            var txt = $('<textarea/>').appendTo($('<div/>').appendTo(card));
        }
        $('#current-date').text(dateStr(curDay()));
        startStop(false);
    }

    var loadWorkspace = function(date) {
        var url = "/workspace/data";
        if (def(date))
            url += '?date=' + (typeof date == 'string' ? date : dateStr(date));
        $.getJSON(url, function(d) {
            d.current = parseDate(d.current);
            d.started = parseDate(d.started);
            d.stopped = parseDate(d.stopped);
            for (var i = d.days.length; --i >= 0;)
                d.days[i].date = parseDate(d.days[i].date);
            ws = d;
            showTasks(ws);
        });
    }

    var copyData = function(d) {
        var r = Array.isArray(d) ? new Array() : {};
        for (var i in d) {
            var v = d[i];
            if (!def(v)) continue;
            if (v instanceof Date)
                v = dateStr(v, 19);
            else if (typeof v == 'object')
                v = copyData(v);

            if (Array.isArray(d))
                r.push(v);
            else
                r[i] = v;
        }
        return r;
    }

    var saveWorkspace = function() {
        var data = JSON.stringify(copyData(ws));
        $.post("/workspace/save", data, function(d) {
            if (d.error)
                alert(d.error);
            else if (d.ok)
                changed(false);
        }, "json");
    }

    var changed = function(ch) { // something has changed
        chg = ch;
        $('.fn-save-ws').css('display', ch ? '' : 'none');
    }

    var checkToday = function() {
        if (def(ws.started) && !today(ws.started)) {
            set('started', null);
            set('stopped', null);
            return true;
        }
        return false;
    }
    var startStop = function(toggle) { // start / stop the clock
        checkToday();
        if (def(ws.started)) {
            if (def(ws.stopped)) {
                if (toggle) {
                    set('started', new Date(now().getTime() - ws.stopped.getTime() + ws.started.getTime()));
                    set('stopped', null);
                }
            }
            else if (toggle) {
                set('stopped', now());
            }
        }
        else {
            if (toggle) {
                set('started', now());
                set('stopped', null);
            }
        }
        tick();
    }

    var tick = function() {
        var t;
        var s = false;
        checkToday();
        if (def(ws.started)) {
            if (def(ws.stopped))
                t = ws.stopped;
            else {
                t = now();
                setTimeout(tick, 1000);
                s = true;
            }
            t = '[ ' + timeStr(Math.trunc((t - ws.started.getTime()) / 1000)) + ' ]';
        }
        else
            t = 'Start';

        $('.fn-start-stop a').text(t).toggleClass('clock-running', s);
    }

    var guiInit = function() {
        $('#heaven').click(function() {window.location = '/workspace';});
        $('#ctrl #new-task').click(function() { newTask({pinned: true}, true)});
        $('.fn-save-ws a').click(function() { saveWorkspace(); })
        $('.fn-start-stop a').click(function() { startStop(true); })
        // date navigation
        $('#prev-week').click(function() { loadWorkspace(addDay(curDay(), -7)); })
        $('#next-week').click(function() { loadWorkspace(addDay(curDay(), 7)); })
    }

    guiInit();
    loadWorkspace();
});