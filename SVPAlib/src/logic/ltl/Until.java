package logic.ltl;

import java.util.Collection;
import java.util.HashMap;

import org.sat4j.specs.TimeoutException;

import automata.safa.BooleanExpressionFactory;
import automata.safa.SAFA;
import automata.safa.SAFAInputMove;
import automata.safa.booleanexpression.PositiveBooleanExpression;
import theory.BooleanAlgebra;

public class Until<P, S> extends LTLFormula<P, S> {

	protected LTLFormula<P, S> left, right;

	public Until(LTLFormula<P, S> left, LTLFormula<P, S> right) {
		super();
		this.left = left;
		this.right = right;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Until))
			return false;
		@SuppressWarnings("unchecked")
		Until<P, S> other = (Until<P, S>) obj;
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		return true;
	}

	@Override
	protected PositiveBooleanExpression accumulateSAFAStatesTransitions(
			HashMap<LTLFormula<P, S>, PositiveBooleanExpression> formulaToState, Collection<SAFAInputMove<P, S>> moves,
			Collection<Integer> finalStates, BooleanAlgebra<P, S> ba, int emptyId) {
		BooleanExpressionFactory<PositiveBooleanExpression> boolexpr = SAFA.getBooleanExpressionFactory();

		// If I already visited avoid recomputing
		if (formulaToState.containsKey(this))
			return formulaToState.get(this);

		// Compute transitions for children
		PositiveBooleanExpression leftState = left.accumulateSAFAStatesTransitions(formulaToState, moves, finalStates, ba, emptyId);
		PositiveBooleanExpression rightState =right.accumulateSAFAStatesTransitions(formulaToState, moves, finalStates, ba, emptyId);

		// initialState (l /\ (l U r)) \/ r		
		int id =formulaToState.size();
		PositiveBooleanExpression initialState = boolexpr.MkOr(boolexpr.MkAnd(leftState, boolexpr.MkState(id)), rightState);
		formulaToState.put(this, initialState);

		// delta(l U r, true) = (l /\ (l U r)) \/ r	
		moves.add(new SAFAInputMove<P, S>(id, initialState, ba.True()));
		
		return initialState;
	}

	@Override
	protected boolean isFinalState() {
		return false;
	}

	@Override
	protected LTLFormula<P, S> pushNegations(boolean isPositive, BooleanAlgebra<P, S> ba,
			HashMap<String, LTLFormula<P, S>> posHash, HashMap<String, LTLFormula<P, S>> negHash) throws TimeoutException {
		String key = this.toString();

		LTLFormula<P, S> out = new False<>();

		if (isPositive) {
			if (posHash.containsKey(key)) {
				return posHash.get(key);
			}
			out = new Until<>(left.pushNegations(isPositive, ba, posHash, negHash),
					right.pushNegations(isPositive, ba, posHash, negHash));
			posHash.put(key, out);
			return out;
		} else {
			if (negHash.containsKey(key))
				return negHash.get(key);
			
			// not (A U B) == (not B) W (not A /\ not B) 
			LTLFormula<P, S> rightNeg = right.pushNegations(isPositive, ba, posHash, negHash);
			out = new WeakUntil<>(rightNeg, new And<>(left.pushNegations(isPositive, ba, posHash, negHash), rightNeg));
			negHash.put(key, out);
			return out;
		}
	}

	@Override
	public void toString(StringBuilder sb) {
		sb.append("(");
		left.toString(sb);
		sb.append(" U ");
		right.toString(sb);
		sb.append(")");
	}

	@Override
	public int getSize() {
		return 1 + left.getSize() + right.getSize();
	}
}
