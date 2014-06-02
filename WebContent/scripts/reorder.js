
var _reorderDetails = null;

function addReorder(collection, items, rerender) {
	
	// loop all the drag images
	items.each( function(index) {
		
		// get reference to image
		var img = $(this);
		
		// disable standard drag
		img.on('dragstart', function(event) { event.preventDefault(); });
		
		// add mousedown
		_listeners.push( img.mousedown( {collection: collection, index: index}, function(ev){
			// get a reference to the image
			var img = $(ev.target);
			// retain a reference to the image we have selected
			_reorderDetails = { object: img, collection: ev.data.collection, index: ev.data.index };
		}));
		
		// add mousemove
		_listeners.push( img.mouseover( {collection: collection, index: index, rerender: rerender}, function(ev){
			// get a reference to the potential reorder to image
			var reorderTo = $(this);
			// if there are reorder details from mousing down on a different image
			if (_reorderDetails) {	
				// get a reference to the collection object of the image we've just hit
				var collection = ev.data.collection;
				// if the image we're on is different from the image we started with
				if (_reorderDetails.object[0] !== reorderTo[0]) {
					// only if the object are from the same collection
					if (_reorderDetails.collection === collection) {
						// retain the position we are moving to
						var toIndex = ev.data.index;
						// retain the position we are moving from
						var fromIndex = _reorderDetails.index;						
						// retain the object we are moving as the "from"
						var fromObject = collection[fromIndex];
						// check whether we're replacing up or down
						if (fromIndex > toIndex) {
							// a lower object has been moved up - shift all objects above from down one
							for (var i = fromIndex; i > toIndex ; i--) {
								collection[i] = collection[i - 1];
							}
							// put the from into the to
							collection[toIndex] = fromObject;
						} else {
							// a high object has been moved down - shift all objects below to up one
							for (var i = fromIndex; i < toIndex; i++) {
								collection[i] = collection[i + 1];
							}
							// put the from into the to
							collection[toIndex] = fromObject;
						}
						// make the to object the from in the _reorderDetails to stop constant swapping
						_reorderDetails = { object: $(ev.target), collection: collection, index: ev.data.index };
						// re-render 
						ev.data.rerender();
					}
				}
			}
		}));
		
	});
}
