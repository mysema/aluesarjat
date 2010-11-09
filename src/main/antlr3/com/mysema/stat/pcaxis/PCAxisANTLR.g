grammar PCAxisANTLR;
options {
	output=AST;
	language=Java;
	memoize=false;
	backtrack=false;
}
@header {
package com.mysema.stat.pcaxis;
}
@lexer::header {
package com.mysema.stat.pcaxis;
}
@members{
	private java.util.Map pxMap = new java.util.LinkedHashMap();

    private int dataSize = 1;
}

/*------------------------------------------------------------------
 * PARSER RULES
 *------------------------------------------------------------------*/

px returns [java.util.Map result]
 	:	(map (NL map)+ ( NL | WS )* EOF) {$result = pxMap;};

map scope {Key currentKey;}
	:	mapKey EQ mapValue SCOL {pxMap.put($mapKey.key, $mapValue.values);}; 

mapKey returns [Key key]
	:	KEY (keySpec)? {$map::currentKey = $key = new Key($KEY.text, $keySpec.spec);};

keySpec returns [String spec]
	:	'(' s=TEXT ')' {$spec = PCAxis.convertString($s.text);};

mapValue returns [List values]
	@init {
		$values = PCAxis.DATA.equals($map::currentKey) ? new java.util.ArrayList(dataSize) : new java.util.ArrayList();
	}
	@after {
	   if ("VALUES".equals($map::currentKey.getName())) {
	       dataSize *= $values.size();
	   }
	}
	:	(NL)? value[$values] (delim value[$values])*;
	
value[List values]
	:	v=(TEXT | NUMBER) {$values.add(PCAxis.convert($v.text));};

delim	:	COMMA? (WS | NL)*;

/*------------------------------------------------------------------
 * LEXER RULES
 *------------------------------------------------------------------*/

KEY	:	('A'..'Z') ('A'..'Z' | '-')+;

EQ	:	'=';

SCOL	:	';';

NL	:	'\r'? '\n';

NUMBER	:	'0'..'9'+ ('.' '0'..'9'+)?;

WS	:	' '+;

COMMA	:	',';

TEXT	:	'"' ~'"'* '"';
