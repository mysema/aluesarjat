
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
function getExportCSVLink() {
	var href = "search?";
	for (var i=0; i < restrictions.length; i++) {
		href += "value=" + encodeURIComponent(restrictions[i]) + "&";
	}
	href += "format=csv"
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

function printFacet(facet, template, availableValues) {
	var values = facet.values;
	template.push("<div class='facet' data-id='", facet.id, "'>");
	if (values.length >= 20) {
		template.push("<input class='quicksearch' type='text' data-id='", facet.id, "'/>");
	}
	template.push("<a href='#' class='compare' data-id='", facet.id, "'>compare</a>");
	
	template.push("<h3 class='facetTitle'>", facet.name, "</h3>");
	
	allFacets[facet.id] = facet;

	template.push("<div class='facetValues'>");
	for (var i=0; i < values.length; i++) {
		var value = values[i];
		if (!availableValues || availableValues[value.id]) {
			value.facet = facet;
			template.push("<div class='facetValue visible");
			if (0 <= restrictions.indexOf(value.id)) {
				template.push(" selectedValue");
			}
			template.push("' data-id='", value.id,"' data-facet='", facet.id, "'>", value.name);
			if (value.description) {
				template.push("<img src='images/info.png' alt='Click for more information' class='facetValueInfo' data-id='", value.id,"'/>");
			}
			template.push("</div>");
			allValues[value.id] = value;
		}
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

function strCompare(a, b) {
	if (a < b) return -1;
	else if (b < a) return 1;
	else return 0;
}
function sortRestrictions(restrictions) {
	restrictions.sort(function(a, b) {
		var result =  strCompare(allValues[a].facet.id, allValues[b].facet.id);
		if (result == 0) {
			result = strCompare(a, b);
		}
		return result;
	});
}
function executeQuery() {
	// No restrictions
	if (restrictions.length == 0) {
		$(".facet").show();
		// TODO: Apply quicksearch
		$(".facetValue").show().addClass("visible");
		$(".selectedValue").removeClass("selectedValue");
		$("#results").html("");
		return;
	} 
	
	$("#results").html("<img src='images/ajax-loader.gif' alt='Loading results'/>");
	sortRestrictions(restrictions);
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
				var multiSelection = false;
				restrictionFacets[facet.id] = true;
				template.push("<tr><th class='restriction'>", facet.name, ":</th><td colspan='",headersLength,"'>");

				do {
					restriction = allValues[restrictions[i]];
					template.push("<div class='facetValue selectedValue' data-id='", 
							restriction.id, "'>", restriction.name);
					if (restriction.description) {
						template.push("<img src='images/info.png' alt='Click for more information' class='facetValueInfo' data-id='", restriction.id,"'/>");
					}
					template.push("</div>");
					i++;
					if (i < restrictions.length) {
						restriction = allValues[restrictions[i]];
						if (facet.id != restriction.facet.id) {
							restriction = null;
							i--;
						} else {
							multiSelection = true;
						}
					} else {
						restriction = null;
					}
				} while (restriction != null);
				
				if (!multiSelection) {
					usedFacets.push(facet.id);
				}
				
				template.push("<a href='#' class='compare' data-id='", facet.id, "'>compare</a>");
				template.push("</td></tr>");
				
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
				template.push("<th>Value</th>");
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
				
				template.push(" - <a href='javascript: window.location = getExportCSVLink();'>Export CSV</a>");
				
				$("#sizes").show();
			}else{
				$("#sizes").hide();
			}
			
			$("#results").html(template.join(""));
			
		}
	});	
}

function quicksearch(event) {
	var id = $(event.target).data("id");
	var text = $(event.target).val().toUpperCase();
	$(event.target).parent().find(".facetValue").each(function() {
		var value = $(this);
		if (value.hasClass("visible")) {
			if (text.length == 0) {
				value.show();
			} 
			else if (0 <= value.text().toUpperCase().indexOf(text)) {
				value.show();
			} 
			else if (!value.hasClass("selectedValue")) {
				value.hide();
			}
		}
	});
}

function compare(event) {
	var id = $(event.target).data("id");

	sortRestrictions(restrictions);
	
	var comparisonRestrictions = [];
	for (var i=0; i < restrictions.length; i++) {
		if (allValues[restrictions[i]].facet.id != id) {
			comparisonRestrictions.push(restrictions[i]);
		}
	}
	var template = [];
	var facet = allFacets[id]
	var popup = $("#popup");

	if (comparisonRestrictions.length == 0) {
		printFacet(facet, template);
		popup.find("#popupContent").html(template.join(""));
	} else {
		popup.find("#popupContent").html("<img src='images/ajax-loader.gif' alt='Loading results'/>");
		$.ajax({
			url: "search", 
			datatype: "json",
			data: {"value": comparisonRestrictions, "include": ["values"]},
			error: function(xhr, textStatus, errorThrown) {
				popup.find("#popupContent").html(xhr.responseText);
			},
			success: function(data){
				printFacet(facet, template, data.availableValues);
				popup.find("#popupContent").html(template.join(""));
			}
		});
	}
	
	popup.jqm({
		onHide: function(hash) {
			var changed = false;
			$("#popup .facetValue").each(function () {
				var valueId = $(this).data("id");
				var rindex = restrictions.indexOf(valueId);
				var checked = $(this).hasClass("selectedValue");
				if (checked) {
					if (rindex < 0) {
						restrictions.push(valueId);
						changed = true;
					}
				} else {
					if (0 <= rindex) {
						restrictions.splice(rindex,1);
						changed = true;
					}
				}
				restrictions.push();
			});
			// Remove this listener
			popup.jqm({onHide:null});
			hash.w.hide(); // Hide
			hash.o.remove(); // Hide
			if (changed) {
				executeQuery();
			}
			return true;
		}
	});
	popup.jqmShow();
}
function getSparqlLink() {
	return "sparql.html?query=" + encodeURIComponent(toSparql());
}
function toSparql() {
	var facetToRestrictions = {};
	for (var i=0; i < restrictions.length; i++) {
		var r = restrictions[i];
		var f = r.substring(0, r.indexOf(':'));
		if (!facetToRestrictions[f]) {
			facetToRestrictions[f] = [];
		}
		facetToRestrictions[f].push(r);
	}
	var sparql = ["SELECT ?item ?value ?dimension\n",
	              "WHERE {\n",
	              "  ?item rdf:type scv:Item;\n",
	              "        rdf:value ?value;\n",
	              "        scv:dimension ?dimension"];
	var filter;

	for (f in facetToRestrictions) {
		var facetRestrictions = facetToRestrictions[f];
		if (facetRestrictions.length == 1) {
			sparql.push(";\n        scv:dimension ", facetRestrictions[0]);
		} else {
			sparql.push(";\n        scv:dimension ?", f);
			
	
			if (filter) {
				filter.push("\n  && (");
			} else {
				filter = ["(\n  "];
			}
			for (var i=0; i < facetRestrictions.length; i++) {
				if (0 < i) {
					filter.push(" || ");
				}
				filter.push("?", f, " = ", facetRestrictions[i]);
			}
			filter.push(")");
		}
	}
	sparql.push(".\n")
	if (filter) {
		sparql.push("FILTER (");
		sparql = sparql.concat(filter);
		sparql.push(")\n");
	}
	sparql.push("}");
	return sparql.join("");
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

	$("#popup").jqm();
	
	$(".facetValueInfo").live("click",function(event) {
		var id = $(event.target).data("id");
		var value = allValues[id];
		if (value.description) {
			var popup = $("#popup");
			popup.find("#popupContent").html("<h3>" + value.name + "</h3>" + value.description.replace(/\n/g, "</br>"));
			popup.jqmShow();
//			popup.show();
		}
		return false;
	});
	
	$("input.quicksearch").live("keyup", quicksearch);
	
	$(".compare").live("click", compare);
	
	$("#help").click(function(){
		var popup = $("#popup");
		popup.find("#popupContent").html("<h3>Help</h3>" + $("#helptext").html());
		popup.show();
	});
	
	$("#facets .facetValue, #results .facetValue").live("click",function(event) {
		var id = $(event.target).data("id");
		offset = 0;
		
		var i = restrictions.indexOf(id);
		if (i < 0) {
			restrictions.push(id);
		} else {
			restrictions.splice(i,1);
		}
		
		executeQuery();
	});
	
	$("#popup .facetValue").live("click",function(event) {
		var target = $(event.target);
		if (target.hasClass("selectedValue")) {
			target.removeClass("selectedValue");
		} else {
			target.addClass("selectedValue");
		}
	});
	
	$("#popupClose").click(function(event) {
		$(this).parent().jqmHide();
	});
	
	// Change page size
	$("#pageSize").change(function() {
		offset = 0;
		limit = new Number($(this).val());
		executeQuery();
	});

});


