
String.prototype.startsWith = function(str) {return (this.match("^"+str)==str)}

if(!Array.indexOf){
	Array.prototype.indexOf = function(obj){
	  for(var i=0; i<this.length; i++){
	     if(this[i]===obj){
	        return i;
	     }
	  }
	  return -1;
	}
}

jQuery.ajaxSettings.traditional = true;

var restrictions = [];
var allFacets = {};
var allValues = {};
var limit = 50;
var offset = 0;

function getRequestParameters() {
	var parameters = {};
	if (window.location.search.length > 1) {
		var query = window.location.search.substring(1);
		var vars = query.split("&");
		for (var i=0; i<vars.length; i++) {
			var pair = vars[i].split("=");
			// First entry with this name
			if (typeof parameters[pair[0]] === "undefined") {
				parameters[pair[0]] = [ decodeURIComponent(pair[1]) ];
			} 
			// If second or later entry with this name
			else {
				parameters[pair[0]].push( decodeURIComponent(pair[1]) );
			}
		} 
	}
	return parameters;
}

function getBookmarkLink() {
	var href = window.location.href;
	href = href.substring(0, href.length - window.location.search.length);
	href += "?limit=" + limit + "&offset=" + offset;
	for (var i=0; i < restrictions.length; i++) {
		href += "&value=" + encodeURIComponent(restrictions[i]);
	}
	return href;
}

function getFacetName(facetId) {
	return allFacets[facetId].name;
}

function getValueName(valueId) {
	return allValues[valueId].name;
}

function getFacetId(facetValue) {
	return allValues[facetValue].facet.id;
}

function printFacet(facet, template) {
	var values = facet.values;
	template.push("<div class='facet' id='", toID(facet.id), "' data-id='", facet.id, "'>");
	
	if (values.length >= 20) {
		template.push("<input class='quicksearch' type='text' data-id='", facet.id, "'/>");
	}
	
	template.push("<h3 class='facetTitle'>", facet.name, "</h3>");
	allFacets[facet.id] = facet;

	template.push("<div class='facetValues'>");
	for (var i=0; i < values.length; i++) {
		var value = values[i];
		value.facet = facet;
		template.push("<div class='facetValue visible' id='", toID(value.id), "' data-id='", value.id,"' data-facet='", facet.id, "'>", value.name);
		if (value.description) {
			template.push("<img src='images/info.png' alt='Click for more information' class='facetValueInfo' data-id='", value.id,"'/>");
		}
		template.push("</div>");
		allValues[value.id] = value;
	}
	template.push("</div></div>");
}

function fromID(id) {
	if (id.startsWith("restriction_")) {
		id = id.substring(12);
	} else if (id.startsWith("itemValue_")) {
		id = id.substring(10);
	}
	return id.replace('-', ':');
}

function toID(prefixed) {
	return prefixed.replace(':', '-');
}

function nextPage() {
	offset += limit;
	executeQuery();
}

function prevPage() {
	if (offset > 0) {
		offset -= limit;
		if (offset < 0) {
			offset = 0;
		}
		executeQuery();
	}
}

function executeQuery() {
	$("#results").html("<img src='images/ajax-loader.gif' alt='Loading results'/>");
	$.ajax({
		url: "search", 
		datatype: "json",
		data: {"value": restrictions, "include": ["items", "values"], "limit": limit, "offset": offset},
		error: function(xhr, textStatus, errorThrown) {
			$("#results").html(xhr.responseText);
		},
		success: function(data){
			var template = [];

			template.push("<table class='items'><thead>");
			
			var usedFacets = [];
			
			// RESTRICTIONS
			var restrictionFacets = {};
			var headersLength = data.headers ? data.headers.length : 10;
			for (var i=0; i < restrictions.length; i++) {
				var restriction = allValues[restrictions[i]];
				var facet = restriction.facet;
				restrictionFacets[facet.id] = true;
				template.push("<tr><th class='restriction'>", facet.name, ":</th><td colspan='",headersLength,"'><div class='facetValue selectedValue' data-id='", 
						restriction.id, "'>", restriction.name);
				if (restriction.description) {
					template.push("<img src='images/info.png' alt='Click for more information' class='facetValueInfo' data-id='", restriction.id,"'/>");
				}
				template.push("</div></td></tr>");
				
				usedFacets.push(facet.id);
			}
			
			template.push("<tr>");
			
			var columns = [];
			
			// FACETS
			if (data.availableValues) {
				var visibleValues = data.availableValues;
				
				$(".facetValue").each(function () {
					var id = $(this).data("id");
					if (restrictions.indexOf(id) >= 0) {
						$(this).addClass("selectedValue");
						$(this).addClass("visible");
						$(this).show();
					} else if (visibleValues[id] == true) {
						$(this).removeClass("selectedValue");
						$(this).addClass("visible");
						$(this).show();
					} else {
						$(this).removeClass("selectedValue");
						$(this).removeClass("visible");
						$(this).hide();
					}
				});
				
				$(".facet").each(function () {
					// Hide empty facets
					if ($(this).find(".visible").length > 0) {
						$(this).show();
					} else {
						$(this).hide();
					}
				});
			}
			
			// ITEMS
			if (data.items) {
				var headers = data.headers;
				
				var skippedColumns = [];
				
				for (var i=0; i < headers.length; i++) {
					if ($.inArray(headers[i], usedFacets) == -1){
						template.push("<th>", getFacetName(headers[i]), "</th>");	
					}else{
						skippedColumns.push(i);
					}
				}
				template.push("<th>Arvo</th>");
				template.push("</tr></thead>");
				
				var items = data.items;

				template.push("<tbody>");
				
				var previousValues = [];
				for (var i=0; i < items.length; i++) {
					var item = items[i];
					var values = item.values;
					template.push("<tr class='itemRow ", (i % 2 == 0 ? "odd" : "even"),"'>")

					var columnValues = []; // [value 1 ... value N, value, unit]
					
					columnValues[headers.length] = "<td><div class='itemValue'> = " + item.value + "</div></td>";

					for (var j=0; j < values.length; j++) {
						var value = allValues[values[j]];
						
						if ($.inArray(j, skippedColumns) > -1){
							continue;
						}
						
						if (!value) {
							columnValues[j] = "<td>&nbsp;</td>";
							continue;
						}
						
						var facet = value.facet;
						var extraClass = "";
						
						var columnTemplate = [];
						columnTemplate.push("<td><div class='facetValue", extraClass, "' data-id='", value.id, "'>", value.name);
						
						if (value.description) {
							columnTemplate.push("<img src='images/info.png' alt='Click for more information' class='facetValueInfo' data-id='", value.id, "'/>");
						}
						columnTemplate.push("</div></td>");
						columnValues[j] = columnTemplate.join("");
					}
					
					template.push(columnValues.join(""));
					template.push("</tr>");
					previousValues = values;
				}
				template.push("</tbody>");
								
			} else {
				template.push("</tr></thead>");
			}
			
			template.push("</table>");
			
			if (data.items){								
				// navigation
				if (offset > 0) {
					template.push("<a href='javascript: prevPage();'>Previous page</a> - ");
				} else {
					template.push("Previous page - ");
				}
				if (data.hasMoreResults) {
					template.push("<a href='javascript: nextPage();'>Next page</a>");
				} else {
					template.push("Next page");
				}
				
				$("#sizes").show();
			}else{
				$("#sizes").hide();
			}
			
			$("#results").html(template.join(""));
			
		}
	});	
}

$(document).ready(function(){

	var parameters = getRequestParameters();
	if (parameters.value) {
		restrictions = parameters.value;
	}
	if (parameters.limit) {
		limit = new Number(parameters.limit[0]);
	}
	if (parameters.offset) {
		offset = new Number(parameters.offset[0]);
	}

	$.ajax({
		url: "facets", 
		datatype: "json", 
		error: function(xhr, textStatus, errorThrown){
			$("#results").html(xhr.responseText);
		},
		success: function(data){
			var template = [];

			var facets = data.facets;
			for (var i=0; i < facets.length; i++) {
				printFacet(facets[i], template);
			}
			
			$("#facets").html(template.join(""));

			if (restrictions.length) {
				executeQuery();
			}
		}
	});

	
	$(".facetValueInfo").live("click",function(event) {
		var id = $(event.target).data("id");
		var value = allValues[id];
		if (value.description) {
			var popup = $("#popup");
			popup.html("<h3>" + value.name + "</h3>" + value.description.replace(/\n/g, "</br>"));
			popup.show();
		}
		return false;
	});
	
	$("input.quicksearch").live("keyup",function(event) {
		var id = $(event.target).data("id");
		var text = $(event.target).val();
		$("#" + toID(id) + " .facetValue").each(function() {
			var value = $(this);
			if (value.hasClass("visible")) {
				if (text.length == 0) {
					value.show();
				} 
				// starts with or has a word starting with given text
				else if (new RegExp("^" + text + "|[ -\\(\\)\\.\\+/]" + text, "i").test(value.text())) {
					value.show();
				} 
				else if (!value.hasClass("selectedValue")) {
					value.hide();
				}
			}
		});
	});
	
	$("#help").click(function(){
		var popup = $("#popup");
		popup.html("<h3>Help</h3>" + $("#helptext").html());
		popup.show();
	});
	
	$("#popup").click(function(event){
		$(this).hide();
		return false;
	});
	
	$(".facetValue").live("click",function(event) {
		var id = $(event.target).data("id");
		offset = 0;
		
		var i = restrictions.indexOf(id);
		if (i < 0) {
			restrictions.push(id);
		} else {
			restrictions.splice(i,1);
		}
		
		if (restrictions.length == 0) {
			$(".facet").show();
			// TODO: Apply quicksearch
			$(".facetValue").show().addClass("visible");
			$(".selectedValue").removeClass("selectedValue");
			$("#results").html("");
		} else {
			executeQuery();
		}
	});
	
	// Change page size
	$("#pageSize").change(function() {
		offset = 0;
		limit = new Number($(this).val());
		executeQuery();
	});

});


