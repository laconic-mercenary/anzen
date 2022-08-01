/**
 * common functionality class
 */
var anzen = anzen || function() { };

/**
 * Detects whether browser is on a client device that is considered 'mobile'. 
 */
anzen.isMobileClient = anzen.isMobileClient || function() {
	if (navigator.userAgent.match(/Android/i)
			|| navigator.userAgent.match(/webOS/i)
			|| navigator.userAgent.match(/iPhone/i)
			|| navigator.userAgent.match(/iPad/i)
			|| navigator.userAgent.match(/iPod/i)
			|| navigator.userAgent.match(/BlackBerry/i)
			|| navigator.userAgent.match(/Windows Phone/i)) {
		return true;
	} else {
		return false;
	}
};

/** 
 * Performs a redirect.
 * @param ctx This will typically be request.contextPath
 * @param relPath An example would be /home/home.jsf
 */
anzen.redirectTo = anzen.redirectTo || function(ctx, relPath) {
	window.location.replace(ctx + relPath);
};
