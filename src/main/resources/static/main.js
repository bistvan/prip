$(function() {
    var ws = {tasks: []};
    var chg = false;
    var errTO = null;
    var autoSaveTO = null;
    var autoSaveBlock = false;


    var showError = function(msg) {
        $('.error-message').css('display', '').find('span').text(msg);
        if (def(errTO))
            clearTimeout(errTO);
        errTO = setTimeout(function() {$('.error-message').hide(400);errTO = null}, 3500);
    }

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
    var spentTime = function(t) {
        if (t < 60)
            return '1 min.';
        if (t < 3600)
            return Math.round(t / 60) + ' mins.';
        return (Math.round(t / 360) / 10) + ' hours';
    }
    var wsDay = function(d) { // workspace day
        if (!def(d)) d = new Date();
        for (var i = ws.days.length; --i >= 0;) {
            var day = ws.days[i];
            if (sameDay(day.date, d))
                return day;
        }
        return null;
    }
    var sameDay = function(d1, d2) {
        return d1.getYear() == d2.getYear() && d1.getMonth() == d2.getMonth() && d1.getDate() == d2.getDate();
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
        return Math.trunc(sec / 3600) + ':' + pad(Math.trunc(sec % 3600 / 60)) + ':' + pad(Math.trunc(sec % 60));
    }
    var dateStr = function(d, x) {
        var t = d.getTime() - (d.getTimezoneOffset() * 60000);
        return new Date(t).toISOString().slice(0, def(x) ? x : 10);
    }

    var logWork = function(tid,sec,descr,pref) {
        if (def(tid)) {
            var day = wsDay(curDay());
            if (def(day)) {
                var tx = descr.val();
                if (def(pref) && tx.length > 0 && !tx.startsWith(pref))
                    tx = pref + tx;
                var act = tid + ',' + (def(sec) ? sec : '') + ',' + tx;
                descr.val('');
                set('activities', def(day.activities) && day.activities.length > 0 ? day.activities + '\n' + act : act, day);
                $('.rep-sum-d' + day.day + ' textarea').val(day.activities);
                resetClock();
            }
            else {
                showError('Cannot edit: ' + curDay());
            }
        }
        else
            showError('Task has no ID; Save first');
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
                            var found = false;
                            for (var i = 0; !found && i < ws.tasks.length; i++) {
                                if (ws.tasks[i].id == s.id) {
                                    found = true;
                                    showError('The ' + name.val() + ' is already pinned.');
                                }
                            }
                            if (!found) {
                                name.val(d.name = s.name);
                                descr.val(d.descr = s.descr);
                                est.val(d.estimate = s.estimate);
                                id.text('#' + (d.id = s.id));
                            }
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
        var logTime = $('<button type="button" title="Log with time" class="btn btn-link btn-sm log-with-time"/>').text('LogT').appendTo($('<span>').appendTo(ctrl))
            .click(function() {
                if (!def(ws.started))
                    showError('Timer is not started.');
                else
                    logWork(d.id, def(ws.started) ? Math.trunc(((def(ws.stopped) ? ws.stopped : now()).getTime() - ws.started) / 1000) : null, text);
            });
        var log = $('<button href=# title="Log without time" class="btn btn-link btn-sm log"/>').text('Log').appendTo($('<span>').appendTo(ctrl))
            .click(function() {
                logWork(d.id, null, text);
            });
        var log = $('<button href=# title="Record commit hash" class="btn btn-link btn-sm log"/>').text('#com').appendTo($('<span>').appendTo(ctrl))
            .click(function() {
                if (text.val().length == 0)
                    showError('Set an activity description.');
                else
                    logWork(d.id, null, text, '#');
            });
        var unpin = $('<button href=# title="Unpin this task" class="btn btn-secondary btn-sm unpin"/>').text('Unpin').appendTo($('<span>').appendTo(ctrl))
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
        var workTime = 0;
        for (var i = 1; i < 7; i++) {
            var date = addDay(start, i);
            var wday = wsDay(date);
            if (!def(wday))
                ws.days.push(wday = {date: date, activities:''});
            wday.day = i;
            showDaytask(date, wday);
            if (def(wday.activities)) {
                var rows = wday.activities.split('\n');
                var len = 0;
                for (var j = 0; j < rows.length; j++) {
                    var a = rows[j].split(',');
                    if (a.length >= 2) {
                        var time = a[1];
                        var mul = 1;
                        if (time.endsWith("m"))
                            mul = 60;
                        if (time.endsWith("h"))
                            mul = 3600;
                        if (mul > 1)
                            time = time.substring(0, time.length - 1);
                        var sec = parseInt(time) * mul;
                        if (!isNaN(sec))
                            workTime += sec;
                    }
                }
            }
        }
        $('.work-hours i').text('(hours: ' + timeStr(workTime) + ')');
        $('#current-date').text(dateStr(curDay()));
        startStop(false);
    }

    var showDaytask = function(date, wday) {
        var t = $('<div class="col col-md-2"/>').addClass('rep-sum-d' + wday.day).appendTo('#report-sum');
        if (sameDay(date, curDay()))
            t.addClass('current-day');
        var cd = dateStr(date);
        var card = $('<div class="card shadow-sm c-report-sum"/>').appendTo(t);
        var ctrl = $('<div/>').appendTo(card);
        var a = $('<button type="button" title="Select date" class="btn btn-link btn-sm select-day">').text(cd).data('date', date).appendTo(ctrl)
            .click(function() { switchDay( $(this).data('date') ); });
        var cl = $('<button type="button" title="Clear text" class="btn btn-link btn-sm">').text('Clear').appendTo(ctrl).click(function() {
            txt.val('');
            set('activities', '', wday);
        });
        var txt = $('<textarea class="form-control"/>').val(wday.activities).appendTo($('<div/>').appendTo(card)).change(function() {
            set('activities', txt.val(), wday);
        });
    }

    var switchDay = function(date) {
        set('current', date);
        showTasks(ws);
    }

    var loadWorkspace = function(date) {
        if (chg) {
            showError('Unsaved changes found. Save first, or refresh the browser');
            return;
        }
        var url = "/workspace/data";
        if (def(date))
            url += '?date=' + (typeof date == 'string' ? date : dateStr(date));
        $.getJSON(url, function(d) {
            d.current = parseDate(d.current);
            d.started = parseDate(d.started);
            d.stopped = parseDate(d.stopped);
            if (!def(d.days))
                d.days = [];
            for (var i = d.days.length; --i >= 0;)
                d.days[i].date = parseDate(d.days[i].date);
            ws = d;

            $('#settings-panel input').each(function() {
                var ed = $(this);
                var id = ed.prop('id').substring(3);
                if (ed.prop('type') == 'checkbox') {
                    ed.prop('checked', def(ws[id]) && ws[id]).change(function() {
                        set(id, ed.prop('checked'));
                    });
                }
                else {
                    ed.val(def(ws[id]) ? ws[id] : '').change(function() {
                        set(id, ed.val());
                    });
                }
            });
            showTasks(ws);
            $('#download-monthly').attr('href', '/workspace/monthlyReport.xls?date=' + dateStr(curDay())).show();
            changed(false);
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

    var clearAutosave = function() {
        if (def(autoSaveTO)) {
            clearTimeout(autoSaveTO);
            autoSaveTO = null;
        }
    }

    var saveWorkspace = function() {
        clearAutosave();
        $(':focus').blur();// drop focus
        var data = JSON.stringify(copyData(ws));
        $.post("/workspace/save", data, function(d) {
            if (d.error) {
                showError(d.error);
                autoSaveBlock = true;
            }
            else if (d.ok) {
                autoSaveBlock = false;
                changed(false);
                loadWorkspace(); // reload all
            }
        }, "json");
    }

    var changed = function(ch) { // something has changed
        chg = ch;
        $('.fn-save-ws').css('display', ch ? '' : 'none');
        clearAutosave();
        if (ch && ws.autosave && !autoSaveBlock)
            autoSaveTO = setTimeout(function() { saveWorkspace(); }, 30000);
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

    var resetClock = function(clean) {
        if (def(clean)) {
            set('started', null);
            set('stopped', null);
            tick();
        }
        else {
        if (def(ws.started))
            ws.started = now();
            if (def(ws.stopped))
                ws.stopped = now();
        }
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
            $('.fn-reset').css('display', '');
        }
        else {
            t = 'Start';
            $('.fn-reset').css('display', 'none');
        }

        $('.fn-start-stop a').text(t).toggleClass('clock-running', s);
    }

    var showProgressReport = function(date) {
        var url = "/workspace/report?date=" + (typeof date == 'string' ? date : dateStr(date));
        $.getJSON(url, function(d) {
            if (d.error) { alert(d.error); return; }
            $('#hide-prip').show();
            var subj = $('#weekly-report #subject').text(def(d.pripSubject) ? d.pripSubject : '');
            var r = $('#weekly-report #body').empty();

            if (def(ws.supervisor))
            $('<h2>').text('Hi ' + ws.supervisor + '!').appendTo(r);
            if (d.chunks.length > 0) {
                $('<p>').text('This is my progress report from: ' + d.chunks[0].interval + '.').appendTo(r);
                $('<p>').text('Thanks, ' + ws.devName).appendTo(r);

                showTable(d.chunks[0]).appendTo(r);
                showDetails(d.chunks[0]).appendTo(r);
            }
            if (d.chunks.length > 1) {
                $('<h3>').text('Schedule for next week: ' + d.chunks[1].interval + ':').appendTo(r);
                showTable(d.chunks[1]).appendTo(r);
            }
            $('#weekly-report').show(400);
        });
    }
    var showTable = function(chunk) {
        var t = $('<table class="prip-tab" cellspacing="0" cellpadding="4"/>');
        var start = def(chunk.days[0].activities) && chunk.days[0].activities.length > 0 ? 0 : 1;
        var end = chunk.days.length;
        if (!def(chunk.days[end - 1].activities) || chunk.days[end - 1].activities.length == 0) end--;

        var head = $('<tr style="color: #ffffff;" class="prip-head"/>').appendTo(t);
        var rows = [];
        var maxRows = 0;
        for (var i = start; i < end; i++) {
            var day = chunk.days[i];
            if (def(day.activities) && maxRows < day.activities.length)
                maxRows = day.activities.length;
        }
        for (var i = start; i < end; i++) {
            var day = chunk.days[i];
            $('<td/>').text(day.name).appendTo(head);
            var l = def(day.activities) ? day.activities.length : 0;
            if (l) {
                for (var j = 0; j < l; j++) {
                    var ar;
                    if (rows.length <= j) rows.push(ar = $('<tr/>').appendTo(t)); else ar = rows[j];
                    var td = $('<td/>').append($.parseHTML(day.activities[j])).appendTo(ar);
                    if (j == l - 1 && l < maxRows)
                        td.attr('rowspan', maxRows - l + 1);
                }
            }
            else {
                var ar;
                if (rows.length == 0) rows.push(ar = $('<tr/>').appendTo(t)); else ar = rows[0];
                var td = $('<td/>').text(day.activities[j]).appendTo(ar).attr('rowspan', maxRows);
            }

        }
        return t;
    }

    var showDetails = function(chunk) {
        var r = $('<div class="prip-details"/>');
        for (var i = 0; i < chunk.tasks.length; i++) {
            var task = chunk.tasks[i];
            var tb = $('<p class="task-block">').appendTo(r);
            $('<span class="task-title">').append($.parseHTML(task.title)).appendTo(tb); tb.append('<br/>');
            if (def(task.activities)) {
                for (var j = 0; j < task.activities.length; j++) {
                    $('<span class="task-activity">').append($.parseHTML(task.activities[j])).appendTo(tb); tb.append('<br/>');
                }
            }
            if (def(task.spentTime)) {
                $('<span class="task-spent">').text('Spent time: ' + spentTime(task.spentTime)).appendTo(tb); tb.append('<br/>');
            }
            if (def(task.commits)) {
                $('<span class="task-commit">').text('Commits: ').append($.parseHTML(task.commits)).appendTo(tb); tb.append('<br/>');
            }

        }
        return r;
    }

    var guiInit = function() {
        $('#heaven').click(function() {window.location = '/workspace';});
        $('#ctrl #new-task').click(function() { newTask({pinned: true}, true); autoSaveBlock = true; });

        // main menu
        $('.fn-save-ws a').click(function() { saveWorkspace(); });
        $('.fn-start-stop a').click(function() { startStop(true); });
        $('.fn-reset a').click(function() { resetClock(true); });
        $('.fn-settings a').click(function() {
            if ($("#settings-panel").css('display') == 'none') {
                $("#settings-panel").show( 400 );
            }
            else {
                $("#settings-panel").hide( 400 );
            }
        });
        $('.action-hide').click(function() {
            var e = $(this);
            var d = e.attr('data-depth');
            if (def(d)) for (var i = parseInt(d); --i > 0;)
                e = e.parent();
            e.hide(400);
        });

        // date navigation
        $('#prev-week').click(function() { loadWorkspace(addDay(curDay(), -7)); })
        $('#next-week').click(function() { loadWorkspace(addDay(curDay(), 7)); })
        $('#go-today').click(function() { loadWorkspace(now()); })
        $('#show-prip').click(function() { showProgressReport(curDay()); })
        $('#hide-prip').click(function() { $(this).css('display', 'none'); $('#weekly-report').hide(400); })

        // autoselect
        $("#weekly-report #subject, #weekly-report #body").on('mouseup', function() {
        	var sel, range;
        	var el = $(this)[0];
        	if (window.getSelection && document.createRange) { //Browser compatibility
        	  sel = window.getSelection();
        	  if(sel.toString() == ''){ //no text selection
        		 window.setTimeout(function(){
        			range = document.createRange(); //range object
        			range.selectNodeContents(el); //sets Range
        			sel.removeAllRanges(); //remove all ranges from selection
        			sel.addRange(range);//add Range to a Selection.
        		},1);
        	  }
        	}else if (document.selection) { //older ie
        		sel = document.selection.createRange();
        		if(sel.text == ''){ //no text selection
        			range = document.body.createTextRange();//Creates TextRange object
        			range.moveToElementText(el);//sets Range
        			range.select(); //make selection.
        		}
        	}
        });
    }

    guiInit();
    loadWorkspace();
});