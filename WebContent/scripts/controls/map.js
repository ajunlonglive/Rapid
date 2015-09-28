// Many thanks to http://www.movable-type.co.uk/scripts/latlong-gridref-v1.html

/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */
/*  Convert latitude/longitude <=> OS National Grid Reference points (c) Chris Veness 2005-2010   */
/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */


/*
 * convert OS grid reference to geodesic co-ordinates
 */

// this has been modified from the original to accept E and N directly, and return a simple object with lat/lng properties

function OSGridToLatLong(E, N) {
  
  var a = 6377563.396, b = 6356256.910;              // Airy 1830 major & minor semi-axes
  var F0 = 0.9996012717;                             // NatGrid scale factor on central meridian
  var lat0 = 49*Math.PI/180, lon0 = -2*Math.PI/180;  // NatGrid true origin
  var N0 = -100000, E0 = 400000;                     // northing & easting of true origin, metres
  var e2 = 1 - (b*b)/(a*a);                          // eccentricity squared
  var n = (a-b)/(a+b), n2 = n*n, n3 = n*n*n;

  var lat=lat0, M=0;
  do {
    lat = (N-N0-M)/(a*F0) + lat;

    var Ma = (1 + n + (5/4)*n2 + (5/4)*n3) * (lat-lat0);
    var Mb = (3*n + 3*n*n + (21/8)*n3) * Math.sin(lat-lat0) * Math.cos(lat+lat0);
    var Mc = ((15/8)*n2 + (15/8)*n3) * Math.sin(2*(lat-lat0)) * Math.cos(2*(lat+lat0));
    var Md = (35/24)*n3 * Math.sin(3*(lat-lat0)) * Math.cos(3*(lat+lat0));
    M = b * F0 * (Ma - Mb + Mc - Md);                // meridional arc

  } while (N-N0-M >= 0.00001);  // ie until < 0.01mm

  var cosLat = Math.cos(lat), sinLat = Math.sin(lat);
  var nu = a*F0/Math.sqrt(1-e2*sinLat*sinLat);              // transverse radius of curvature
  var rho = a*F0*(1-e2)/Math.pow(1-e2*sinLat*sinLat, 1.5);  // meridional radius of curvature
  var eta2 = nu/rho-1;

  var tanLat = Math.tan(lat);
  var tan2lat = tanLat*tanLat, tan4lat = tan2lat*tan2lat, tan6lat = tan4lat*tan2lat;
  var secLat = 1/cosLat;
  var nu3 = nu*nu*nu, nu5 = nu3*nu*nu, nu7 = nu5*nu*nu;
  var VII = tanLat/(2*rho*nu);
  var VIII = tanLat/(24*rho*nu3)*(5+3*tan2lat+eta2-9*tan2lat*eta2);
  var IX = tanLat/(720*rho*nu5)*(61+90*tan2lat+45*tan4lat);
  var X = secLat/nu;
  var XI = secLat/(6*nu3)*(nu/rho+2*tan2lat);
  var XII = secLat/(120*nu5)*(5+28*tan2lat+24*tan4lat);
  var XIIA = secLat/(5040*nu7)*(61+662*tan2lat+1320*tan4lat+720*tan6lat);

  var dE = (E-E0), dE2 = dE*dE, dE3 = dE2*dE, dE4 = dE2*dE2, dE5 = dE3*dE2, dE6 = dE4*dE2, dE7 = dE5*dE2;
  lat = lat - VII*dE2 + VIII*dE4 - IX*dE6;
  var lon = lon0 + X*dE - XI*dE3 + XII*dE5 - XIIA*dE7;

  return {lat:lat.toDeg(), lng:lon.toDeg() };
}


/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */

/*
 * extend Number object with methods for converting degrees/radians
 */
Number.prototype.toRad = function() {  // convert degrees to radians
  return this * Math.PI / 180;
}
Number.prototype.toDeg = function() {  // convert radians to degrees (signed)
  return this * 180 / Math.PI;
}

/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */


/*

Copyright (C) 2014 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
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

//an array to manage the in-page maps	                
var _maps = [];	    
//a global for the geo-coder (if required)
var _geocoder = null;

// get the standard map position object a data object a varierty of ways, including the async geocoder
function getMapPosition(data, rowIndex, callBack, map, details) {
	// create a basic position object
	var pos ={s:null};
	// check the data object
	if (data && data.fields && data.rows && data.rows.length > rowIndex) {
		// loop the fields
		for (var i in data.fields) {
			// get the field
			var f = data.fields[i];
			// if there is a field
			if (f) {
				// check for latitude
				if (f == "lat" || f == "latitude") pos.lat = data.rows[rowIndex][i];
				// check for longditude
				if (f == "lng" || f == "lon" || f == "longitude") pos.lng = data.rows[rowIndex][i];
				// do the easting checks
				if (f == "e" || f == "east" || f == "easting" || f == "eastings") pos.e = data.rows[rowIndex][i];
				// do the northing checks
				if (f == "n" || f == "north" || f == "northing" || f == "northings") pos.n = data.rows[rowIndex][i];
				// do the address or search checks
				if (f == "search" || f == "address") pos.s = data.rows[rowIndex][i];
				// do the title checks
				if (f == "title") pos.title = data.rows[rowIndex][i];
				// do the info checks
				if (f == "info") pos.info = data.rows[rowIndex][i];
			}
		}
		// if not lat and lng 
		if (!pos.lat && !pos.lng) {
			// if there is e and
			if (pos.e && pos.n) {
				// if we had eastings and northings, convert
				var latlng = OSGridToLatLong(pos.e, pos.n);
				pos.lat = latlng.lat;
				pos.lng = latlng.lng;
			} else {
				// create the geocoder if we don't have one already
				if (!_geocoder) _geocoder = new google.maps.Geocoder();
				// if there is not currently a search term make it the first cell
				if (!pos.s) pos.s = data.rows[rowIndex][0];
				// if there is a callback
				if (callBack) {
					// geocode the search term
					_geocoder.geocode( {address: pos.s}, function(results, status) {
					    if (status == google.maps.GeocoderStatus.OK) {							
							for (var i in results) {
								var result = results[i];
								if (result.geometry && result.geometry.location) {
									var l = result.geometry.location;
									pos.lat = l.lat();
									pos.lng = l.lng();								
									callBack(map, pos, details, data, rowIndex);								
								}
							}      	
						} 
					 });
				}
			}
		}		
	}
	return pos;	
}

// set the map centre, used by both the setData method and the getPosition callback
function setMapCentre(map, pos) {
	if (map && pos && pos.lat && pos.lng) {		
		var latlng = new google.maps.LatLng(pos.lat, pos.lng);
		map.panTo( latlng );
	}
}

// add a map marker, used by both the addMapMarkers function (from the properties), and the getPosition callback
function addMapMarker(map, pos, details, data, rowIndex) {
	if (map && pos && pos.lat && pos.lng) {		
		var markerOptions = {
			map: map,
			position: new google.maps.LatLng(pos.lat, pos.lng)				
		};
		if (pos.title) markerOptions.title = pos.title;
		if (details.markerImage) markerOptions.icon = "applications/" + _appId + "/" + _appVersion + "/" + details.markerImage;
		var marker = new google.maps.Marker(markerOptions);	
		marker.index = map.markers.length;
		marker.data = {fields:data.fields,rows:[data.rows[rowIndex]]};
		map.markers.push(marker);					
		if (pos.info) {
			var markerInfoWindow = new google.maps.InfoWindow({
				content: pos.info
			});
			google.maps.event.addListener(marker, 'click', function() {
			    markerInfoWindow.open(map,marker);
			});
		}
		if (details.markerClickFunction) {
			google.maps.event.addListener(marker, 'click', function() {
				map.markerSelectedIndex = marker.index;
			    window[details.markerClickFunction]($.Event('markerclick'));
			});
		}
	}	
}

// adds markers to a map, used by add markers and replace markers
function addMapMarkers(map, data, details) {
	if (data && data.rows) {
		for (var i in data.rows) {
			var pos = getMapPosition(data, i, addMapMarker, map, details);
			addMapMarker(map, pos, details, data, i);
		}
	}
}            