"use strict";

console.debug('WORKER: executing.');

// rapid pwa namesapce
var _rapidpwa = {
	online: function() {
		return false;
	}
}

/* A version number is useful when updating the worker logic,
	 allowing you to remove outdated cache entries during the update.
*/
var _swVersion = 'v1';

/* These resources will be downloaded and cached by the service worker
	 during the installation process. If any resource fails to be downloaded,
	 then the service worker won't be installed either.
*/

// these are resources that we do not expect to change much, we will request them when the service worker installs but not refresh the cache with the latest online fetch
var _rapidResources = [
	"offline.htm",
	"manifest.json",
	"favicon.ico",
	"images/RapidLogo.svg",
	"images/RapidLogo_72x72.png",
	"images/RapidLogo_96x96.png",
	"images/RapidLogo_128x128.png",
	"images/RapidLogo_144x144.png",
	"images/RapidLogo_152x152.png",
	"images/RapidLogo_192x192.png",
	"images/RapidLogo_384x384.png",
	"images/RapidLogo_512x512.png",
	"images/grid.svg",
	"images/right.svg",
	"images/tool.svg",
	"scripts/designlinks.js",
	"scripts/jquery-3.3.1.js",
	"scripts/jquery-ui-1.12.1.js",
	"scripts/extras.js",
	"scripts/json2.js",
	"scripts/controls/map.js",
	"scripts_min/bootstrap.min.js",
	"scripts_min/bootstrap.js",
	"scripts_min/jquery-3.3.1.min.js",
	"scripts_min/jquery-3.3.1.js",
	"scripts_min/jquery-ui-1.12.1.min.js",
	"scripts_min/jquery-ui-1.12.1.js",
	"scripts_min/jquery-touchSwipe-1.6.18.min.js",
	"scripts_min/jquery-touchSwipe-1.6.18.js",
	"styles/bootstrap.css",
	"styles/designlinks.css",
	"styles/index.css",
	"styles/fonts/fontawesome/css/font-awesome.css",
	"styles/fonts/fontawesome/fonts/fontawesome-webfont.woff",
	"styles/fonts/fontawesome/fonts/fontawesome-webfont.ttf?v=4.2.0",
	"styles/fonts/OpenSans-Regular.woff",
	"styles/fonts/OpenSans-Regular.woff2",
	"styles/fonts/rapid/font-rapid.css",
	"index.jsp",
	"~?action=getApps"
];

var _rapidResourceFolders = [
	"images/",
	"scripts/",
	"scripts_min/",
	"styles/",
	"styles_min/"
];

var _appResources = [];

// these resources must be sychronised with the server when they are available - if online look for them in fetches and update cache, use cache if offline
var _refreshes = [
	"~?action=getApps"
];

var _trimUrls = [".js", ".css", ".woff", ".woff2", ".ttf"];

var _appStatus;

var _contextPath;


/* The fetch event fires whenever a page controlled by this service worker requests
	 a resource. This isn't limited to `fetch` or even XMLHttpRequest. Instead, it
	 comprehends even the request for the HTML page on first load, as well as JS and
	 CSS resources, fonts, any images, etc.
*/
self.addEventListener("fetch", function(event) {
	
	// derive the context path from the sw.js location
	_contextPath = location.href.replace("sw.js", "");
	
	// get the url from the event request
	var url = event.request.url;
	// get the method from the event request
	const method = event.request.method;
	
	// proceed to direct server response if request is for service worker
	if (url.endsWith("sw.js")) return;
	
	// proceed to direct server response  if request is for designer
	if (url.startsWith(_contextPath + "designer") || url.includes("a=designer")) return;
	
	// proceed to direct server response  if request is for design page
	if (url.includes("designPage.jsp")) return;
	
	// proceed to direct server response  if request is for admin app
	if (url.startsWith(_contextPath + "rapid") || url.includes("a=rapid")) return;
	
	// if request is for root or index
	if (url === _contextPath || url.endsWith("index.jsp")) {
		event.respondWith(
			new Promise((resolve, reject) =>
				fetchAndCache(_contextPath + "index.jsp", { redirect: "manual" })
				.then(resolve)
				.catch(_ => resolve(getFromCache(_contextPath + "index.jsp")))
			)
		);
		return;
	}
	
	// if request is for login
	if (url.endsWith("login.jsp") && method === "GET") {
		event.respondWith(
			new Promise((resolve, reject) =>
				fetchOrReject(url, { redirect: "manual" })
				.then(resolve)
				.catch(_ => resolve(getFromCache(_contextPath + "offline.htm")))
			)
		);
		return;
	}
	
	// if request is for logout
	if (url.endsWith("logout.jsp")) {
		event.respondWith(
			new Promise((resolve, reject) =>
				fetchOrReject(url, { redirect: "manual" })
				.then(resolve)
				.catch(_ => resolve(getFromCache(_contextPath + "index.jsp")))
			)
		);
		return;
	}
	
	// if request is for getApps
	if (url.endsWith("~?action=getApps")) {
		event.respondWith(
			new Promise((resolve, reject) =>
				fetchAndCache(url, { method: "POST", redirect: "manual" })
				.then(resolve)
				.catch(_ => resolve(getFromCache(_contextPath + "~?action=getApps")))
			)
			.then(response => response.json())
			.then(apps =>
				Promise.all(apps.map(app =>
					isAppInCache(app.id).then(appIsCached => {
						app.isCached = appIsCached;
						return app;
					})
				))
			)
			.then(apps => new Response(new Blob([JSON.stringify(apps)], {type : "application/json"}), { statusText: "OK", url: _contextPath + "~?action=getApps" }))
		);
		return;
	}
	
	
	// We only check the cache for GET requests to the Rapid server, unless it's part of what we want to refresh each time
	if (url && url.startsWith(_contextPath) && (event.request.method === "GET" || url.endsWith("~?action=getApps"))) {
		
		if (url.endsWith("sw.js")) event.respondWith(fetch(event.request));
		
		// set the standard fetch options
		var fetchOptions = { redirect: "manual" };
		
		// special fetch options for getApps
		if (url.endsWith("~?action=getApps")) {
			fetchOptions.method = "POST";
			fetchOptions.redirect = "follow";
		}
		
		var urlParts = url.split("?");
		var urlParameters = urlParts[1] || ""; // end of the url with parameter assignments
		var parameterAssignments = urlParameters.split("&") // array of parameter assignments
		var parameters = {};
		parameterAssignments
		.filter(assignment => assignment !== "")
		.forEach(assignment => {
			var nameValue = assignment.split("="); // array of name and value
			parameters[nameValue[0]] = nameValue[1];
		});
		
		var altUrlComponents = url.match(_contextPath.replace(/\//g, "\\/") + "(\\w+)$");
		var altUrlAppId = altUrlComponents && altUrlComponents[1];
		var appId = parameters.a || altUrlAppId;
		
		// if requesting an app
		if (appId) {
			var versionParameter = parameters.v ? "&v=" + parameters.v : "";
			var appResourcesUrl = _contextPath + "~?a=" + appId + "&action=resources";
			var latestAppVersion = appVersionCached(appId);
			// check for app updates to update cache
			fetch(appResourcesUrl, fetchOptions)
			.then(response => {
				if (response.ok && !response.url.endsWith("login.jsp")) {
					return response.json()
					.then(resources => {
						_appStatus = resources.status;
						if (resources.resources && !parameters.v) {
							var urlsToCache = resources.resources.map(url => (_contextPath.startsWith("http") ? "" : _contextPath) + url);
							caches.open(_swVersion + 'offline').then(cache => {
								cache.match(urlsToCache[0]).then(response => {
									// if this app version is not cached, do
									if (!response) {
										removeAppResources(appId)
										.then(_ => updateCache(urlsToCache));
									}
								})
							});
						}
					})
				}
			})
			.catch(reason => console.log("WORKER: failed getting app resources: " + reason));
		}
		var dialogueParameter = url.includes("action=dialogue") ? "&action=dialogue" : "";
		
		// remove all url parameters, except for the page ($1)
		url = url.replace(/(p=P\d+).*$/, "$1");
		
		url = url + dialogueParameter;
		if (dialogueParameter) _appResources.push(url);
		
		console.debug('WORKER: fetch event in progress for ' + event.request.method, event.request.url);
		/*   Similar to event.waitUntil in that it blocks the fetch event on a promise.
			 Fulfilment result will be used as the response, and rejection will end in a
			 HTTP response indicating failure.
		*/
		
		// use the modified url as the cache key
		event.respondWith(
			caches.open(_swVersion + 'offline').then(cache =>
				
				cache.keys().then(keys => {
					
					var cachedUrls = keys
						.filter(key => key.url.match("a=" + altUrlAppId + "[&$]"))
						.map(key => key.url)
						.sort();
					
					if (altUrlAppId && cachedUrls[0]) url = cachedUrls[0];
					
					return cache.match(url).then(cachedResponse => {
						// respond with a previously cached response, falling back to a freshly fresh response, caching the fresh response
						
						var freshResponseWithCachedOfflineFallback = new Promise((resolve, reject) =>
							fetch(url, fetchOptions )
							.then(freshResponse => {
								if (freshResponse && (freshResponse.ok || freshResponse.type === "opaqueredirect")) {
									var clonedResponse = freshResponse.clone()
									if (_rapidResourceFolders.concat(_rapidResources).concat(_appResources).some(res => url.includes(res))
											&& !freshResponse.redirected) {
										cache.put(url, clonedResponse)
											.then(_ => resolve(freshResponse));
									} else {
										resolve(freshResponse);
									}
								} else if (freshResponse.status === 404) {
									fetch(_contextPath + "page404.htm", { redirect: "follow" }).then(resolve);
								} else if (freshResponse.status === 500) {
									fetch(_contextPath + "page500.htm", { redirect: "follow" }).then(resolve);
								} else if (!_trimUrls.some(ext => url.endsWith(ext))) {
									var fallback = _contextPath + (url === _contextPath ? "index.jsp" : "offline.htm");
									cache.match(fallback).then(page =>
										page ? resolve(page) : reject("WORKER: could not fetch resource and offline page was unavailable")
									);
								} else {
									reject("WORKER: could not fetch resource");
								}
							})
							.catch(reason => {
								if (!_trimUrls.some(ext => url.endsWith(ext))) {
									var fallback = _contextPath + (url === _contextPath ? "index.jsp" : "offline.htm");
									cache.match(fallback).then(page =>
										page ? resolve(page) : reject("WORKER: could not fetch resource and offline page was unavailable")
									);
								}
							})
						).catch(reason => console.log(reason));
						
						if (_appStatus === "development") {
							return new Promise((resolve, reject) => {
								fetch(url).then(response => {
									if (response && (response.ok || response.type === "opaqueredirect")) {
										resolve(response);
									} else if (response.status === 404) {
										fetch(_contextPath + "page404.htm").then(resolve);
									} else if (response.status === 500) {
										fetch(_contextPath + "page500.htm").then(resolve);
									} else {
										resolve(cachedResponse ? cachedResponse : resolve(getFromCache(_contextPath + "offline.htm")));
									}
								}).catch(_ => resolve(cachedResponse ? cachedResponse : resolve(getFromCache(_contextPath + "offline.htm"))));
							})
						} else {
							return cachedResponse || freshResponseWithCachedOfflineFallback;
						}
					});
				})
			)
		); // event respondWith
		
	} else {
		
		// If we don't block the event as shown above, then the request will go to the network as usual.
		console.debug('WORKER: non-GET fetch event ignored. ' + event.request.method, event.request.url);
		
	} // GET check
		
});

/* 
The install event fires when the service worker is first installed.
You can use this event to prepare the service worker to be able to serve
files while visitors are offline.
*/
self.addEventListener("install", function(event) {
	
	console.debug('WORKER: install event in progress.');
	
	_contextPath = location.href.replace("sw.js", "");
	
	// Using event.waitUntil(p) blocks the installation process on the provided promise. If the promise is rejected, the service worker won't be installed.
	event.waitUntil(
		 updateCache(_rapidResources)
		 .then(function() {
			 console.debug('WORKER: install completed');
		 })
	);
});

function updateCache(resourcesToCache) {
	
	// The caches built-in is a promise-based API that helps you cache responses, as well as finding and deleting them.
	// You can open a cache by name, and this method returns a promise. 
	// We use a versioned cache name here so that we can remove old cache entries in one fell swoop later, when phasing out an older service worker.
	return caches
	.open(_swVersion + 'offline')
	.then(cache => {
		// After the cache is opened, we can fill it with the offline files.
		// The pattern below will continue to add requests to the cache even if some of them fail
		return Promise.all(
			// loop the resources array passing in the url
			resourcesToCache.map(function (url) {
				var fetchOptions = { redirect: "follow", mode: "no-cors" };
				if (url.endsWith("~?action=getApps")) fetchOptions.method = "POST";
				// fetch the url (we do this rather than use the add method, so we can check the response codes
				fetch(url, fetchOptions)
				.then(response => {
					// if we got a response and the code is 200 - we want to ignore redirects and authentication failures
					if (response && response.ok) {
						return cache.put(url, response)
						.then(() => {
							_appResources.push(url);
							console.debug('WORKER: added to cache: ' + url);
						})
						.catch(reason =>  console.error('WORKER: failed to cache ' + url + ' : ' + String(reason)));
					} else {
						return console.debug('WORKER: not caching ' + url + ' : response status ' + response.status);
					}
				
				}).catch(reason => console.error('WORKER: failed to fetch ' + url + ' : ' + String(reason)));
			})
		);
	})
}

/* 
The activate event fires after a service worker has been successfully installed.
It is most useful when phasing out an older version of a service worker, as at
this point you know that the new worker was installed correctly. In this example,
we delete old caches that don't match the version in the worker we just finished
installing.
*/
self.addEventListener("activate", function(event) {
	/* Just like with the install event, event.waitUntil blocks activate on a promise.
		 Activation will fail unless the promise is fulfilled.
	*/
	console.debug('WORKER: activate event in progress.');

	_contextPath = location.href.replace("sw.js", "");
	
	event.waitUntil(
		caches
		// This method returns a promise which will resolve to an array of available cache keys.
		.keys()
		.then(function (keys) {
			// We return a promise that settles when all outdated caches are deleted.
			return Promise.all(
				keys
				.filter(key => !key.startsWith(_swVersion))
				.map(key => caches.delete(key)));
		})
		.then(function() {
			console.debug('WORKER: activate completed.');
		})
	);
});


function removeAppResources(appId) {
	
	var appFilter = key =>
		key.match(_contextPath.replace(/\//g, "\\/") + appId + "$")
		|| key.includes("a=" + appId)
		|| key.includes("applications/" + appId);
	
	_appResources = _appResources.filter(appFilter);
	
	return caches.open(_swVersion + 'offline').then(cache =>
		cache.keys()
		.then(keys =>
			Promise.all(
				keys.filter(key => appFilter(key.url))
				.map(key => cache.delete(key))
			)
		)
	);
}

function appVersionCached(appId) {
	return caches.open(_swVersion + 'offline').then(cache =>
		cache.keys()
		.then(keys => {
			var key = keys.find(key => key.url.includes("&v=") && key.url.includes(appId))
			var components = key && key.url.match("&v=(\\w+)[$&]");
			return components && components[1]
		})
	);
}

// many thanks to http://craig-russell.co.uk/2016/01/29/service-worker-messaging.html

// messages from clients
self.addEventListener('message', function(event) {
    
	console.log("SW received message: " + event.data);
	// check for json
	if (event.data && (event.data.startsWith("{") || event.data.startsWith("["))) {
		
		// get the data
		var data = JSON.parse(event.data);
		if ("page" in data) {
			
			if (data["page"] == "index.jsp") {
				
				var apps = data.data;
				for (var i in apps) {
					
					var app = apps[i];
					event.waitUntil(
						fetch('~?a=' + app['id'] + '&action=getPages',{
							method: 'POST',
							headers: {
					            'Content-Type': 'application/json'
					        },
							body : "{}"
						}).then(function(response){
							console.log( response.json() );
						})
					);
					
					
				}
				
			}
			
		}
		
	}
    
});


function fetchOrReject(url, options) {
	return new Promise((resolve, reject) => {
		fetch(url, options).then(freshResponse => {
			if (freshResponse && (freshResponse.ok || freshResponse.type === "opaqueredirect")) {
				resolve(freshResponse);
			} else {
				reject("Fetch failed for: " + url);
			}
		})
		.catch(reject);
	});
}

function fetchAndCache(url, options) {
	return new Promise((resolve, reject) => {
		fetchOrReject(url, options)
		.then(freshResponse => {
			const clonedResponse = freshResponse.clone();
			if (freshResponse.url === url) {
				caches.open(_swVersion + 'offline').then(cache =>
					cache.put(url, clonedResponse)
				);
			}
			resolve(freshResponse);
		})
		.catch(reject);
	});
}

function getFromCache(url) {
	return new Promise((resolve, reject) => {
		caches.open(_swVersion + 'offline')
		.then(cache => cache.match(url))
		.then(cachedResponse => {
			if (cachedResponse) {
				resolve(cachedResponse);
			} else {
				reject("Nothing in cache for: " + url);
			}
		})
	});
}

function addToCache(urls) {
	return caches.open(_swVersion + 'offline')
	.then(cache =>
		cache.addAll(urls)
	);
}

function removeOtherAppVersionsFromCache(appName, version) {
	return caches.open(_swVersion + 'offline')
	.then(cache => cache.keys())
	.then(keys =>
		keys.filter(key => {
			const a = getUrlParameter(key.url, "a");
			const v = getUrlParameter(key.url, "v");
			return a && a === appName && (!v || v !== version);
		})
		.map(key => cache.delete(key))
	);
}

function isAppInCache(appName) {
	return caches.open(_swVersion + 'offline')
	.then(cache => cache.keys())
	.then(keys =>
		!!keys.find(key => {
			const a = getUrlParameter(key.url, "a");
			return a && a === appName;
		})
	);
}

function qualify(url) {
	if (!url.startsWith("http")) url = _contextPath + url;
	return url;
}

function getUrlParameter(url, parameterName) {
	const matches = url.match("[?&]" + parameterName + "=([^&]+)[&$]");
	return matches && matches[1];
}