(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
    (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
        m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
})(window,document,'script','//www.google-analytics.com/analytics.js','ga');

ga('create', 'UA-85775854-1', 'auto', {
    'allowLinker': false
});

ga('send', 'pageview');


var trackEvent = function(elems, category, action, fnLabel){
    for(var i=0; i < elems.length; i++) {
        elems[i].onclick = function(e){
            console.log("sending event - category: "+ category + " action: "+ action + " etext: "+ e.target);
            ga('send', 'event', category, action, fnLabel(e));
        };
    };
};
