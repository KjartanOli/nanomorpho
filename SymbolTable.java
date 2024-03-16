import java.util.HashMap;
import java.util.Deque;
import java.util.ArrayDeque;

class SymbolTable {
	private void expected(String _a) { return; }
	private Deque<HashMap<String, Integer>> scopes = new ArrayDeque<HashMap<String, Integer>>();

	public int varCount() {
		var count = 0;
		for (var scope : scopes)
			count += scope.size();

		return count;
	}

	public void pushScope() {
		scopes.push(new HashMap<String, Integer>());
	}

	public void popScope() {
		scopes.pop();
	}

	public void addVar(String name) {
		if (scopes.peek().get(name) != null)
			expected("undeclared variable name");
		scopes.peek().put(name, this.varCount());
	}

	public int findVar(String name) {
		for (var scope : scopes) {
			var pos = scope.get(name);
			if (pos != null)
				return pos;
		}
		expected("declared variable name");
		// unreachable
		return 0;
	}
}
