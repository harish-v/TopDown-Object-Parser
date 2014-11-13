import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/* 		OO PARSER AND BYTE-CODE GENERATOR FOR TINY PL
 
Grammar for TinyPL (using EBNF notation) is as follows:

 program ->  decls stmts end
 decls   ->  int idlist ;
 idlist  ->  id { , id } 
 stmts   ->  stmt [ stmts ]
 cmpdstmt->  '{' stmts '}'
 stmt    ->  assign | cond | loop
 assign  ->  id = expr ;
 cond    ->  if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
 loop    ->  while '(' rexp ')' cmpdstmt  
 rexp    ->  expr (< | > | =) expr
 expr    ->  term   [ (+ | -) expr ]
 term    ->  factor [ (* | /) term ]
 factor  ->  int_lit | id | '(' expr ')'
 
Lexical:   id is a single character; 
	      int_lit is an unsigned integer;
		 equality operator is =, not ==

Sample Program: Factorial
 
int n, i, f;
n = 4;
i = 1;
f = 1;
while (i < n) {
  i = i + 1;
  f= f * i;
}
end

   Sample Program:  GCD
   
int x, y;
x = 121;
y = 132;
while (x != y) {
  if (x > y) 
       { x = x - y; }
  else { y = y - x; }
}
end

GREATEST COMMON DIVISOR

int x, y;
x = 121;
y = 132;
while (x != y) {
if (x > y) 
        { x = x - y; }
else
        { y = y - x; }
}
end
 
MAXIMUM OF THREE NUMBERS
 
int i, j, k, m;
i = 1;
j = 111;
k = 11111;
if (i < j) {
   if (j < k)
     { m = k; }
   else
      { m = j; }
} else {
   if (i < k)
      { m = k;  }
   else
       { m = i; }
}
end
 
PRIME NUMBER TESTING
 
int n, s, i, p;
n = 61;
s = 1;
while (s*s < n) {
      s = s + 1;
}
i = 2;
p = 1;
while (i < s+1) {
      if (p = 1) 
           { if ( (n / i ) * i = n)
                 { p = 0; } 
           } 
     i = i + 1;
}
end
 

 */

public class Parser {
	public static void main(String[] args)  {
		System.out.println("Enter program and terminate with 'end'!\n");
		Lexer.lex();
		Program p = new Program();
		Code.output();
	}
}

class Program {
	 Decls decl;
	 Stmts stmts;

	 public Program(){
		 decl = new Decls();
		 stmts = new Stmts();
		 if(Lexer.nextToken==Token.KEY_END){
			 Code.gen("return");
		 }else{
			 System.out.println("Program error" + Lexer.nextChar);
		 }
	 }
}

class Decls {
	Idlist idlist;
	
	 public Decls(){
		 if(Lexer.nextToken == Token.KEY_INT){
			 Lexer.lex();
			 idlist = new Idlist();
			 
			 if(Lexer.nextToken == Token.SEMICOLON){
				 Lexer.lex();
			 }else {
				 System.out.println("Decl error");
			 }
		 } else {
			 System.out.println("Decl2 error");
		 }
	 }
}

class Idlist {
	char id;
	static int idCount = 0;
	 public Idlist(){
		 variableDecl();
	 }
	 
	 private void variableDecl(){
		 if(Lexer.nextToken == Token.ID){
			 id = Lexer.ident;
				Code.populateMap(id, idCount++);
//				Code.gen("istore_" + idCount++);

			 Lexer.lex();
			 if(Lexer.nextToken == Token.COMMA){
				 Lexer.lex();
				 variableDecl();
			 }
		 }else{
			 System.out.println("Id Error");
		 }
	 }
}

class Stmts {
	Stmt stmt;
	Stmts stmts;
	
	public Stmts(){
		stmt = new Stmt();
		if ((Lexer.nextToken != Token.KEY_END) && (Lexer.nextToken != Token.RIGHT_BRACE)){
			stmts = new Stmts();
		}
	}
}

class Stmt {
	static char variable = ' ';
	Assign assign;
	Cond cond;
	Loop loop;
	
	public Stmt(){
		switch(Lexer.nextToken){
		case Token.ID:
			variable = Lexer.ident;
			Lexer.lex();
			assign = new Assign();
			if(Code.containsVariable(variable)){
				Code.gen("istore_" + Code.retrieveByteCode(variable));
//			} else {
//				Code.populateMap(variable, idCount);
//				Code.gen("istore_" + idCount++);
			}
			break;
		case Token.KEY_IF:
			Lexer.lex();
			cond = new Cond();
			break;
		case Token.KEY_WHILE:
			Lexer.lex();
			loop = new Loop();
			break;
		}
	}
} 



class Assign {
//	 assign  ->  id = expr ;
	Expr expr;
	
	public Assign(){
		if(Lexer.nextToken==Token.ASSIGN_OP){
			Lexer.lex();
			expr = new Expr();
			if(Lexer.nextToken==Token.SEMICOLON){
				Lexer.lex();
			}else{
				System.out.println("Assignment semicolon error");
			}
		}
		else{
			System.out.println("Assignment error" + Lexer.nextChar);
		}
	}
}

//cond    ->  if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
class Cond {
	 Rexpr rexpr;
	 Cmpdstmt cmpdstmt1;
	 Cmpdstmt cmpdstmt2;
	 
	 public Cond(){
		 if(Lexer.nextToken == Token.LEFT_PAREN){
			 Lexer.lex();
			 rexpr = new Rexpr();
			 int temp = Code.codeptr - 3;
			 
			 if(Lexer.nextToken == Token.RIGHT_PAREN){
				 Lexer.lex();
				 cmpdstmt1 = new Cmpdstmt();
				 if(Lexer.nextToken == Token.KEY_ELSE){
					 Code.instructionSet.put(temp,Code.instructionSet.get(temp)+" "+(Code.codeptr+3));
					 temp= Code.codeptr;
					 Code.codeptr+=3;
					 Lexer.lex();
					 cmpdstmt2 = new Cmpdstmt();
					 Code.instructionSet.put(temp, "goto "+ (Code.codeptr) );
				 }else{
					 Code.instructionSet.put(temp,Code.instructionSet.get(temp)+" "+(Code.codeptr));
				 }
			 } else {
				 System.out.println("If Right paren error");
			 }
		 } else {
			 System.out.println("If Left paren error");
		 }
	 }
}

class Loop {
//	loop    ->  while '(' rexp ')' cmpdstmt  
	Rexpr rexpr;
	Cmpdstmt cmpdstmt;

	 public Loop(){
		 int gotoPointer = Code.codeptr;
		 
		 if(Lexer.nextToken == Token.LEFT_PAREN){
			 Lexer.lex();
			 rexpr = new Rexpr();
			 int temp = Code.codeptr - 3;
			 
			 if(Lexer.nextToken == Token.RIGHT_PAREN){
				 Lexer.lex();
				 cmpdstmt = new Cmpdstmt();
				 Code.gen("goto " + gotoPointer);
				 Code.instructionSet.put(temp,Code.instructionSet.get(temp)+" "+(Code.codeptr+=2));
			 }else{
				 System.out.println("Right parenthesis error in loop");
			 }
		 }
	 }
}

class Cmpdstmt {
	Stmts stmts;
	
	public Cmpdstmt(){
		if(Lexer.nextToken == Token.LEFT_BRACE){
			Lexer.lex();
			stmts = new Stmts();
			
			if(Lexer.nextToken == Token.RIGHT_BRACE){
				Lexer.lex();
			} else {
				System.out.println("Cmpdsmt Right Brace error");
			}
		}
	}
}

class Rexpr {
	Expr expr1;
	Expr expr2;
	
	public Rexpr(){
		expr1 = new Expr();
		
		switch(Lexer.nextToken){
		case Token.GREATER_OP:
			Lexer.lex();
			expr2 = new Expr();
			Code.gen("if_icmple");
			break;
		case Token.LESSER_OP:
			Lexer.lex();
			expr2 = new Expr();
			Code.gen("if_icmpge");
			break;
		case Token.ASSIGN_OP:
			Lexer.lex();
			expr2 = new Expr();
			Code.gen("if_icmpne");
			break;
		case Token.NOT_EQ:
			Lexer.lex();
			expr2 = new Expr();
			Code.gen("if_icmpeq");
			break;
		default:
			System.out.println("Rexpr error");
			break;
		}
		Code.codeptr+=2;
	}
}

class Expr {  
	Term term;
	Expr expr;
	
	public Expr() {
		char op = ' ';
		term = new Term();
		if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
			op = Lexer.nextChar;
			Lexer.lex();
			expr = new Expr();
			Code.gen(Code.opcode(op));
		}
	}	
}

class Term {  
	Factor factor;
	Term term;
	
	public Term() {
		char op = ' ';
		factor = new Factor();
		if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
			op = Lexer.nextChar;
			Lexer.lex();
			term = new Term();
			Code.gen(Code.opcode(op));
		}
	}
}

class Factor {  
	Expr expr;
	public Factor() {
		int i;
		String byteCode = "";
		
		switch (Lexer.nextToken) {
		case Token.INT_LIT: // number
			i = Lexer.intValue;
			Lexer.lex();
			if((0 <= i) && (i <= 5)){
				Code.gen("iconst_" + i);
			} else if ( i > 127){
				Code.gen("sipush " + i);
				Code.codeptr += 2;
			} else {
				Code.gen("bipush " + i);
				Code.codeptr++;
			}
			break;
		case Token.ID:
			Integer code = Code.retrieveByteCode(Lexer.ident);
			if(code != null)
				byteCode = "iload_" + code.toString();
			Code.gen(byteCode);
			Lexer.lex();
			break;
		case Token.LEFT_PAREN: // '('
			Lexer.lex();
			expr = new Expr();
			Lexer.lex(); // skip over ')'
			break;
		default:
			System.out.println("Factor error");
			break;
		}
	}
	 
}

class Code {
	static TreeMap<Integer,String> instructionSet = new TreeMap<Integer,String>();
	static int codeptr = 0;
	static Map<Character,Integer> idMap = new HashMap<Character,Integer>();
	
	public static void populateMap(Character id, int byteCode){
		idMap.put(id, byteCode);
	}
	
	public static Integer retrieveByteCode(Character id){
		return idMap.get(id);
	}
	
	public static boolean containsVariable(Character id){
		return idMap.containsKey(id);
	}
	
	public static void gen(String s) {
		//code[codeptr] = s;
		instructionSet.put(codeptr++, s);
//		codeptr++;
	}
	
	public static String opcode(char op) {
		switch(op) {
		case '+' : return "iadd";
		case '-':  return "isub";
		case '*':  return "imul";
		case '/':  return "idiv";
		default: return "";
		}
	}
	
	public static void output() {
		for (Integer key: instructionSet.keySet())
			System.out.println(key+":"+instructionSet.get(key));
	}
}


