javascript: function iprl5() {
    var d = document,
        z = d.createElement('scr' + 'ipt'), // WHY?
        b = d.body,
        l = d.location;
    try {
        if (!b) throw (0);
        d.title = '(Saving...) ' + d.title;
        z.setAttribute('src', l.protocol + '//www.instapaper.com/j/5mxpahvqvzm8?u=' + encodeURIComponent(l.href) + '&t=' + (new Date().getTime()));
        b.appendChild(z);
    } catch (e) {
        alert('Please wait until the page has loaded.');
    }
}
iprl5();
void(0)

// figure out how javascript scoping works
javascript:(function(){
  var newScript = document.createElement('scr' + 'ipt');
  var url = encodeURIComponent(l.href);
  var title = encodeURIComponent(document.title);
  newScript.type='text/javascript';
  newScript.src = 'http://localhost:3000/js/bookmarklet.js?dummy=' + Math.random()
    + "&url=" + url + "&title=" + title;
  document.body.appendChild(newScript);
})();

"javascript:(function(){
  var newScript = document.createElement('scr' + 'ipt');
  var url = encodeURIComponent(document.location.href);
  var title = encodeURIComponent(document.title);
  var category = encodeURIComponent('%s');
  var userid = encodeURIComponent('%s');
  var scheme = document.location.protocol;
  var port = (scheme === 'http:') ? '3000' : '8443';
  newScript.type='text/javascript';
  newScript.src= scheme +  '//localhost:' + port + '/js/bookmarklet.js?dummy=' + Math.random() + '&url=' + url + '&title=' + title + '&category=' + category + '&userid=' + userid; 
  document.body.appendChild(newScript);
})()"

javascript:(function(){
           var newScript = document.createElement('scr' + 'ipt');
           var url = encodeURIComponent(document.location.href);
           var title = encodeURIComponent(document.title);
           var category = encodeURIComponent('default');
           var userid = encodeURIComponent('1');
           var scheme = document.location.protocol;var port = (scheme === 'http:') ? '80' : '8443';
           newScript.type='text/javascript';newScript.src= scheme +  '//bookmarking.curtis.io:' + port + '/js/bookmarklet.js?dummy=' + Math.random() + '&amp;url=' + url + '&amp;title=' + title + '&amp;category=' + category + '&amp;userid=' + userid;
           document.body.appendChild(newScript);});

javascript:(function(){
           var newScript = document.createElement('scr' + 'ipt');
                    var url = encodeURIComponent(document.location.href);
                             var title = encodeURIComponent(document.title);
                                      var category = encodeURIComponent('default');var userid = encodeURIComponent('1');var scheme = document.location.protocol;var port = (scheme === 'http:') ? '80' : '8443';newScript.type='text/javascript';newScript.src= scheme +  '//bookmarking.curtis.io:' + port + '/js/bookmarklet.js?dummy=' + Math.random() + '&amp;url=' + url + '&amp;title=' + title + '&amp;category=' + category + '&amp;userid=' + userid;document.body.appendChild(newScript);}()

