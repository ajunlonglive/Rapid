"use strict";

/*


Copyright (C) 2020 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk

This file is part of the Rapid Application Platform

Rapid is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version. The terms require you to include
the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

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
var _swVersion = 'v1.8';

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
	"images/RapidLogo_60x40.png",
	"images/RapidLogo_72x72.png",
	"images/RapidLogo_96x96.png",
	"images/RapidLogo_128x128.png",
	"images/RapidLogo_144x144.png",
	"images/RapidLogo_152x152.png",
	"images/RapidLogo_192x192.png",
	"images/RapidLogo_384x384.png",
	"images/RapidLogo_512x512.png",
	"images/data_store.svg",
	"images/grid.svg",
	"images/right.svg",
	"images/tool.svg",
	"images/wand.svg",
	"scripts/designlinks.js",
	"scripts/extras.js",
	"scripts/jquery-3.3.1.js",
	"scripts/jquery-ui-1.12.1.js",
	"scripts/json2.js",
	"scripts_min/bootstrap.min.js",
	"scripts_min/extras.min.js",
	"scripts_min/jquery-3.3.1.min.js",
	"scripts_min/jquery-touchSwipe-1.6.18.min.js",
	"scripts_min/jquery-ui-1.12.1.min.js",
	"styles/bootstrap.css",
	"styles/designlinks.css",
	"styles/index.css",
	"styles/fonts/OpenSans-Regular.ttf",
	"styles/fonts/OpenSans-Regular.woff",
	"styles/fonts/OpenSans-Regular.woff2",
	"styles/fonts/fontawesome/css/font-awesome.css",
	"styles/fonts/fontawesome/fonts/fa-brands-400.eot",
	"styles/fonts/fontawesome/fonts/fa-brands-400.svg",
	"styles/fonts/fontawesome/fonts/fa-brands-400.ttf",
	"styles/fonts/fontawesome/fonts/fa-brands-400.woff",
	"styles/fonts/fontawesome/fonts/fa-brands-400.woff2",
	"styles/fonts/fontawesome/fonts/fa-regular-400.eot",
	"styles/fonts/fontawesome/fonts/fa-regular-400.svg",
	"styles/fonts/fontawesome/fonts/fa-regular-400.ttf",
	"styles/fonts/fontawesome/fonts/fa-regular-400.woff",
	"styles/fonts/fontawesome/fonts/fa-regular-400.woff2",
	"styles/fonts/fontawesome/fonts/fa-solid-900.eot",
	"styles/fonts/fontawesome/fonts/fa-solid-900.svg",
	"styles/fonts/fontawesome/fonts/fa-solid-900.ttf",
	"styles/fonts/fontawesome/fonts/fa-solid-900.woff",
	"styles/fonts/fontawesome/fonts/fa-solid-900.woff2",
	"styles_min/fonts/rapid/font-rapid.min.css",
	"index.jsp",
	"~?action=getApps"
];

var _offlinePageResources = [
	"index.css",
	"RapidLogo.svg",
	"favicon.ico"
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

// extensions that will have any parameters trimmed off when saving/checking for them in the cache, in addition they are used to identify whether a request was for an app and whether to check for its resources
var _trimUrls = [".js", ".css", ".json", ".woff", ".woff2", ".ttf", ".ico", ".svg", ".gif", ".png", ".jpg", ".jpeg", ".pdf"];

// the root context path of the web application deteremined from the loading path of this sw.js file
var _contextPath;

var lastAppUrl;

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
	
	if (!url.startsWith(_contextPath)) return;
	
	url = url.replace(_contextPath, "");
	
	// get the method from the event request
	const method = event.request.method;
	
	// proceed to direct server response if request is for service worker
	if (url.endsWith("sw.js")) return;
	
	// proceed to direct server response if request is a database action etc
	if (method === "POST" && url.includes("act=")) return;
	
	// proceed to direct server response  if request is for designer
	var designerIndicators = ["designer", "design.jsp", "~?a=designer", "designpage.jsp"];
	var adminIndicators = ["rapid", "~?a=rapid"];
	
	if (designerIndicators.concat(adminIndicators).some(indicator => url.startsWith(indicator))) {
		if (method === "GET") {
			
			event.respondWith(new Promise(resolve =>
				fetchOrReject(_contextPath + url, { redirect: "manual" })
				.then(function(response) {
					resolve(response);
				})
				.catch(_ => resolve(
					caches.open(_swVersion + 'offline')
					.then(cache => cache.match("offline.htm"))
				))
			));
		}
		return;
	}
	
	// ignore resources referred by designer.jsp, allowing resources referred by offline.htm
	var referrer = event.request.referrer.replace(_contextPath,"");
	
	if ((designerIndicators.concat(adminIndicators).some(indicator => referrer.includes(indicator)))
		&& !_offlinePageResources.some(resource => url.endsWith(resource))) {
		return;
	}
	
	// proceed to direct server response  if request is for design page
	if (url.includes("designPage.jsp")) return;
	
	// proceed to direct server response  if request is for admin app
	if (url.startsWith("rapid") || url.includes("a=rapid")) return;
	
	// proceed to direct server response  if request is for soa
	if (url.startsWith("soa")) return;
	
	// proceed to direct server response  if request is for an uploaded file
	if (url.includes("/uploads/")) return;
	
	// if request is for root or index
	if (url === "" || url.endsWith("index.jsp")) {
		event.respondWith(
			new Promise((resolve, reject) =>
				fetchAndCache("index.jsp", { redirect: "manual" })
				.then(resolve)
				.catch(_ => resolve(getFromCache("index.jsp")))
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
				.catch(_ => resolve(getFromCache("offline.htm")))
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
				.catch(_ => resolve(getFromCache("index.jsp")))
			)
		);
		return;
	}
	
	// if request is for getApps
	if (url.endsWith("~?action=getApps")) {
		event.respondWith(
			new Promise((resolve, reject) =>
				fetchAndCache(url, { method: "POST", redirect: "manual" })
				.then(r => resolve(r))
				.catch(_ => resolve(getFromCache("~?action=getApps")))
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
			.then(apps => new Response(new Blob([JSON.stringify(apps)], {type : "application/json"}), { statusText: "OK", url: "~?action=getApps" }))
		);
		return;
	}
	
	// ignore image uploads
	if (method === "POST") return;
	
	// We only check the cache for GET requests to the Rapid server, unless it's part of what we want to refresh each time
	if (url && method === "GET") {
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
		
		var altUrlComponents = url.match("(\\w+)(\/(\\w+))*$");
		var notApp = _trimUrls.some(ext => url.endsWith(ext));
		var altUrlAppId = !parameters.a && !notApp && altUrlComponents && altUrlComponents[1];
		var resourceAppId = url.match("applications\/([^\/]+)\/");
		var requestedAppId = parameters.a || altUrlAppId || (resourceAppId && resourceAppId[1]);
		
		if (!parameters.v && altUrlComponents) parameters.v = altUrlComponents[3];
		
		// if   mobdemo/~?a=mobdemo&v=3&p=P3
		// then ~?a=mobdemo&v=3&p=P3
		if (parameters.a) {
			url = "~?" + urlParameters;
			var currentAppUrl = url;
			if ((referrer === "login.jsp" || referrer === "") && lastAppUrl) url = lastAppUrl;
			lastAppUrl = currentAppUrl;
		}
		
		// if   mobdemo/applications/test/3/rapid.css
		// then mobdemo/3/rapid.css
		
		var lowestDirectoryIndex = ["applications", "scripts", "styles", "scripts_min", "styles_min", "images"]
			.map(directory => url.indexOf(directory))
			.filter(directoryIndex => directoryIndex > -1)
			.reduce((lowestDirectoryIndex, directoryIndex) => Math.min(lowestDirectoryIndex, directoryIndex), url.length);
		
		if (lowestDirectoryIndex < url.length) url = url.slice(lowestDirectoryIndex);
		
		var dialogueParameter = url.includes("action=dialogue") ? "&action=dialogue" : "";
		
		// for the cache remove all url parameters, except for the page ($1)
		var cacheUrl = url.replace(/(p=P\d+).*$/, "$1");
		
		// remove any version
		var versionFreeUrl = cacheUrl.replace(/&v=[^&\/]+/, "");
		if (dialogueParameter) cacheUrl = versionFreeUrl;
		
		// if requesting an app
		event.respondWith(
			caches.open(_swVersion + 'offline').then(cache =>
				
				cache.match("last_resources_" + requestedAppId)
				.then(response => response && response.json())
				.then(appResources =>
					
					cache.match(versionFreeUrl).then(cachedResponse => {
						
						var disambiguatedUrl = requestedAppId ? (appResources && appResources.redirects[cacheUrl] || cacheUrl) + dialogueParameter : cacheUrl;
						var onStartPage = parameters.v === undefined && parameters.p === undefined && cacheUrl.split("/").length === 1;
						
						// if this app version is not cached, do
						if (requestedAppId && ((!cachedResponse) || onStartPage) && ![".js", ".css"].some(ext => cacheUrl.endsWith(ext))) {
							
							var requestedAppVersion = requestedAppId && parameters.v || (appResources && appResources.version);
							var versionParameter = (requestedAppVersion && !onStartPage) ? "&v=" + requestedAppVersion : "";
							var appResourcesUrl = _contextPath + "~?a=" + requestedAppId + versionParameter + "&action=resources";
							// check for app updates to update cache
							
							if (navigator.onLine) {
								fetch(appResourcesUrl, fetchOptions)
								.then(freshResponse => {
									if (freshResponse.ok && !freshResponse.url.endsWith("login.jsp")) {
										return freshResponse.json()
										.then(resources => {
											if (resources.resources && ((!appResources) || appResources.modified < resources.modified)) {
												
												return removeAppResourcesByKeys(appResources && appResources.resources || [])
												.then(_ => {
													
													var startUrl = "~?a=" + resources.id + "&v=" + resources.version + "&p=" + (resources.startPageId || "P1");
													
													resources.redirects = {};
													resources.resources.forEach((resource, index) => {
														if (resource.startsWith("~")) {
															const versionFree = resource.replace(/&v=\d+/, "");
															resources.redirects[versionFree] = resource;
															resources.resources[index] = versionFree;
														}
													});
													
													// Ambiguous urls
													[requestedAppId, requestedAppId + "/" + resources.version, "~?a=" + requestedAppId, "~?a=" + requestedAppId + "&v=" + resources.version]
													.forEach(cacheUrl => {
														resources.redirects[cacheUrl] = startUrl;
														resources.resources.push(cacheUrl);
													});
													
													cache.put("last_resources_" + requestedAppId, new Response(new Blob([JSON.stringify(resources)], {type : "application/json"}), { statusText: "OK", cacheUrl: appResourcesUrl }))
													
													if (navigator.onLine) updateCache(resources.resources, resources.redirects);
												});
											}
										});
									}
								});
							}
							return new Promise(resolve =>
								(navigator.onLine ? fetch(_contextPath + url + dialogueParameter, fetchOptions) : Promise.reject())
								.then(resolve)
								.catch(_ => resolve(cache.match(
									(!requestedAppId || onStartPage) ? cacheUrl : "offline.htm"
								)
								.then(response =>
									response || cache.match("offline.htm")
								)))
							);
							
						} else if (appResources && appResources.status === "live") { // live mode (non-development)
							// respond with a previously cached response, falling back to a freshly fresh response, caching the fresh response
							
							var freshResponseWithCachedFallback = new Promise((resolve, reject) =>
								(navigator.onLine ? fetch(disambiguatedUrl, fetchOptions) : Promise.reject())
								.then(freshResponse => {
									if (freshResponse && (freshResponse.ok || freshResponse.type === "opaqueredirect")) {
										if (_rapidResourceFolders.concat(_rapidResources).concat(appResources.resources).some(res => cacheUrl.includes(res))
												&& !freshResponse.redirected) {
											cache.put(cacheUrl, freshResponse.clone())
												.then(_ => resolve(freshResponse));
										} else {
											resolve(freshResponse);
										}
									} else if (freshResponse.status === 404) {
										fetch(_contextPath + "page404.htm", { redirect: "follow" }).then(resolve);
									} else if (freshResponse.status === 500) {
										fetch(_contextPath + "page500.htm", { redirect: "follow" }).then(resolve);
									} else if (!_trimUrls.some(ext => cacheUrl.endsWith(ext))) {
										var fallback = (cacheUrl === "" ? "index.jsp" : "offline.htm");
										cache.match(fallback).then(page =>
											page ? resolve(page) : reject("WORKER: could not fetch resource and offline page was unavailable")
										);
									} else {
										reject("WORKER: could not fetch resource");
									}
								})
								.catch(reason => {
									if (!_trimUrls.some(ext => cacheUrl.endsWith(ext))) {
										var fallback = (cacheUrl === "" ? "index.jsp" : "offline.htm");
										cache.match(fallback).then(page =>
											page ? resolve(page) : reject("WORKER: could not fetch resource and offline page was unavailable")
										);
									}
								})
							).catch(reason => console.log(reason));
							
							return cachedResponse || freshResponseWithCachedFallback;
							
						} else { // development mode / resources
							
							return new Promise((resolve, reject) =>
								(navigator.onLine ? fetch(url, fetchOptions) : Promise.reject())
								.then(freshResponse => {
									if (freshResponse && (freshResponse.ok || freshResponse.type === "opaqueredirect")) {
										var currentAppResources = appResources && appResources.resources || [];
										if (_rapidResourceFolders.concat(_rapidResources).concat(currentAppResources).some(res => cacheUrl.includes(res)) && !freshResponse.redirected
											|| _trimUrls.some(ext => cacheUrl.endsWith(ext))) {
										
											cache.put(cacheUrl, freshResponse.clone())
												.then(_ => resolve(freshResponse));
										} else {
											resolve(freshResponse);
										}
									} else if (freshResponse.status === 404) {
										fetch(_contextPath + "page404.htm").then(resolve);
									} else if (freshResponse.status === 500) {
										fetch(_contextPath + "page500.htm").then(resolve);
									} else {
										resolve(cachedResponse || getFromCache("offline.htm"));
									}
								}).catch(_ => resolve(cachedResponse || getFromCache("offline.htm")))
							);
						}
					}) // cache.match(url)
				) // cache.match("last_resources_" + requestedAppId)
			) // caches.open(_swVersion + 'offline')
			.catch(reason => console.log("WORKER: failed getting app resources: " + reason))
		); // event.respondWith
		
		console.debug('WORKER: fetch event in progress for ' + event.request.method, event.request.url);
		/*   Similar to event.waitUntil in that it blocks the fetch event on a promise.
			 Fulfilment result will be used as the response, and rejection will end in a
			 HTTP response indicating failure.
		*/
		
		return;
		
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

function updateCache(resourcesToCache, redirects) {
	
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
				var fetchOptions = { redirect: "manual" };
				if (url.endsWith("~?action=getApps")) fetchOptions.method = "POST";
				// fetch the url (we do this rather than use the add method, so we can check the response codes
				var redirectedUrl = redirects && redirects[url] || url
				fetch(_contextPath + redirectedUrl, fetchOptions)
				.then(response => {
					// if we got a response and the code is 200 - we want to ignore redirects and authentication failures
					if (response && response.ok) {
						return cache.put(url, response)
						.then(() => {
							console.debug('WORKER: added to cache: ' + url);
						})
						.catch(reason =>  console.error('WORKER: failed to cache ' + url + ' : ' + String(reason)));
					} else {
						return console.debug('WORKER: not caching ' + url + ' : response status ' + response.status);
					}
				
				}).catch(reason => console.debug('WORKER: failed to fetch ' + url + ' : ' + String(reason)));
			})
		);
	})
}

function removeAppResourcesByKeys(resources) {
	return caches.open(_swVersion + 'offline')
	.then(cache =>
		Promise.all(
			resources.map(resource => cache.delete(resource))
		)
	);
}

// many thanks to http://craig-russell.co.uk/2016/01/29/service-worker-messaging.html


function fetchOrReject(url, options) {
	return new Promise((resolve, reject) => {
		fetch(url, options).then(freshResponse => {
			if (freshResponse && (freshResponse.ok || freshResponse.type === "opaqueredirect" || freshResponse.type === "basic")) {
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
			if (freshResponse.url.endsWith(url) && freshResponse.ok) {
				const clonedResponse = freshResponse.clone();
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

function fetchFreshOrCached(url) {
	return new Promise((resolve, reject) =>
		fetchOrReject(url, { redirect: "manual" })
		.then(resolve)
		.catch(failedFetchReason => {
			getFromCache(url)
			.then(resolve)
			.catch(failedCacheReason => reject(failedFetchReason + " AND " + failedCacheReason))
		})
	);
}

function getCachedOrFreshAndCache(url) {
	return new Promise((resolve, reject) =>
		getFromCache(url)
		.then(resolve)
		.catch(failedCacheReason => {
			fetchAndCache(url, { redirect: "manual" })
			.then(resolve)
			.catch(failedFetchReason => reject(failedCacheReason + " AND " + failedFetchReason))
		})
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

function getUrlParameter(url, parameterName) {
	const matches = url.match("[?&]" + parameterName + "=([^&]+)[&$]");
	return matches && matches[1];
}