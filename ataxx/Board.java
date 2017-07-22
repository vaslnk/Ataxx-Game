package ataxx;

/* Author: P. N. Hilfinger, (C) 2008. */


import static ataxx.PieceColor.*;
import static ataxx.GameException.error;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Observable;
import java.util.LinkedList;
import java.util.Arrays;



/** An Ataxx board. The squares are labeled by column (a char value between
 *  'a' - 2 and 'g' + 2) and row (a char value between '1' - 2 and '7'
 *  + 2) or by linearized index, an integer described below.  Values of
 *  the column outside 'a' and 'g' and of the row outside '1' to '7' denote
 *  two layers of border squares, which are always blocked.
 *  This artificial border (which is never actually printed) is a common
 *  trick that allows one to avoid testing for edge conditions.
 *  For example, to look at all the possible moves from a square, sq,
 *  on the normal board (i.e., not in the border region), one can simply
 *  look at all squares within two rows and columns of sq without worrying
 *  about going off the board. Since squares in the border region are
 *  blocked, the normal logic that prevents moving to a blocked square
 *  will apply.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author Yevgen Vasylenko
 */
class Board extends Observable {

    /** For reasons of efficiency in copying the board,
     *  we use a 1D array to represent it, using the usual access
     *  algorithm: row r, column c => index(r, c).
     *
     *  Next, instead of using a 7x7 board, we use an 11x11 board in
     *  which the outer two rows and columns are blocks, and
     *  row 2, column 2 actually represents row 0, column 0
     *  of the real board.  As a result of this trick, there is no
     *  need to special-case being near the edge: we don't move
     *  off the edge because it looks blocked.
     *
     *  Using characters as indices, it follows that if 'a' <= c <= 'g'
     *  and '1' <= r <= '7', then row c, column r of the board corresponds
     *  to board[(c -'a' + 2) + 11 (r - '1' + 2) ], or by a little
     *  re-grouping of terms, board[c + 11 * r + SQUARE_CORRECTION].
     *  TIPS: (Either extend or jump) */
    private final PieceColor[] _board;

    /** Player that is on move. */
    private PieceColor _whoseMove;

    /** Number of moves so far. **/
    private int _numMoves = 0;

    /** Number of jumps.  **/
    private int _numJumps = 0;

    /** Number of red. **/
    private int _numRed = 0;

    /** Number of blue. **/
    private int _numBlue = 0;

    /** Number of squares on a side of the board. */
    static final int SIDE = 7;
    /** Length of a side + an artificial 2-deep border region. */
    static final int EXTENDED_SIDE = SIDE + 4;

    /** Number of non-extending moves before game ends. */
    static final int JUMP_LIMIT = 25;

    /** Storing all moves. **/
    private Stack<Move> _allMoves = new Stack<>();

    /** Storing arrays of SQ indicies that changed color during ith move. **/
    private Stack<ArrayList<Integer>> _changedColorArrays = new Stack<>();

    /** Saving numJumps in case it gets set to zero. **/
    private Stack<Integer> _savedNumJumps = new Stack<>();

    /** A new, cleared board at the start of the game. */
    Board() {
        _board = new PieceColor[EXTENDED_SIDE * EXTENDED_SIDE];
        clear();
    }

    /** A copy of B. */
    Board(Board b) {
        _board = b._board.clone();
        _numMoves = b._numMoves;
        _numJumps = b._numJumps;
        _whoseMove = b.whoseMove();
        _numRed = b._numRed;
        _numBlue = b._numBlue;

    }

    /**Style tbh. @param color color **/
    public void changeWhoseMoveTo(PieceColor color) {
        _whoseMove = color;
    }

    /**Style tbh. **/
    public void makeBorders() {
        int in = 0;
        while (in < 2 * EXTENDED_SIDE) {
            _board[in] = BLOCKED;
            in++;
        }
        int lim = 2;
        while (in < EXTENDED_SIDE * EXTENDED_SIDE - 2 * EXTENDED_SIDE) {
            _board[in] = BLOCKED;
            in++;
            lim--;
            if (lim == 0) {
                in += 7;
                lim = 4;
            }
        }
        while (in <= EXTENDED_SIDE * EXTENDED_SIDE - 1) {
            _board[in] = BLOCKED;
            in++;
        }
    }

    /** Return the linearized index of square COL ROW. */
    static int index(char col, char row) {
        return (row - '1' + 2) * EXTENDED_SIDE + (col - 'a' + 2);
    }

    /** Return the linearized index of the square that is DC columns and DR
     *  rows away from the square with index SQ. */
    static int neighbor(int sq, int dc, int dr) {
        return sq + dc + dr * EXTENDED_SIDE;
    }

    /**Style tbh. @param sq SQ
     * return ret**/
    char sqtoRow(int sq) {
        int result = (sq / EXTENDED_SIDE) - 1;
        return Integer.toString(result).charAt(0);
    }

    /**Style tbh. @param sq SQ
     * return ret**/
    char sqtoCol(int sq) {
        int result = (sq % EXTENDED_SIDE) - 2;
        String finder = "abcdefg";
        return finder.charAt(result);

    }

    /** Clear me to my starting state, with pieces in their initial
     *  positions and no blocks. */
    void clear() {
        for (int i = 0; i < _board.length; i++) {
            _board[i] = EMPTY;
        }
        makeBorders();
        _whoseMove = RED;
        set("a".charAt(0), "1".charAt(0), BLUE);
        set("a".charAt(0), "7".charAt(0), RED);
        set("g".charAt(0), "1".charAt(0), RED);
        set("g".charAt(0), "7".charAt(0), BLUE);
        _numMoves = 0;
        _numRed = 2;
        _numBlue = 2;
        _numJumps = 0;
        _allMoves = new Stack<>();
        _changedColorArrays = new Stack<>();
        setChanged();
        notifyObservers();
    }

    /** Return true iff the game is over: i.e., if neither side has
     *  any moves, if one side has no pieces, or if there have been
     *  MAX_JUMPS consecutive jumps without intervening extends. */
    boolean gameOver() {
        return ((!canMove(BLUE) && !canMove(RED))
                || _numRed == 0
                || _numBlue == 0
                || numJumps() > JUMP_LIMIT);
    }

    /** Return number of red pieces on the board. */
    int redPieces() {
        return numPieces(RED);
    }

    /** Return number of blue pieces on the board. */
    int bluePieces() {
        return numPieces(BLUE);
    }

    /** Return number of COLOR pieces on the board.
     * @param color col
     * @return ret*/
    int numPieces(PieceColor color) {
        if (color == BLUE) {
            return _numBlue;
        } else if (color == RED) {
            return _numRed;
        }
        return -1;
    }

    /**Style tbh.
     * return ret**/
    int numBlocks() {
        int c = 0;
        for (int in = EXTENDED_SIDE * 2 + 2;
             in <= EXTENDED_SIDE * EXTENDED_SIDE - (EXTENDED_SIDE * 2 + 3);
                in++) {
            if (get(in) == BLOCKED) {
                c++;
            }
        }
        return c;
    }

    /**Style tbh.
     * return some **/
    int numEmpty() {
        int c = 0;
        for (int in = EXTENDED_SIDE * 2 + 2;
             in <= EXTENDED_SIDE * EXTENDED_SIDE - (EXTENDED_SIDE * 2 + 3);
             in++) {
            if (get(in) == EMPTY) {
                c++;
            }
        }
        return c;
    }

    /** Increment numPieces(COLOR) by K. */
    private void incrPieces(PieceColor color, int k) {
        if (color == BLUE) {
            _numBlue += k;
        } else if (color == RED) {
            _numRed += k;
        }
    }

    /** The current contents of square CR, where 'a'-2 <= C <= 'g'+2, and
     *  '1'-2 <= R <= '7'+2.  Squares outside the range a1-g7 are all
     *  BLOCKED.  Returns the same value as get(index(C, R)). */
    PieceColor get(char c, char r) {
        return _board[index(c, r)];
    }

    /** Return the current contents of square with linearized index SQ. */
    PieceColor get(int sq) {
        return _board[sq];
    }

    /** Set get(C, R) to V, where 'a' <= C <= 'g', and
     *  '1' <= R <= '7'. */
    private void set(char c, char r, PieceColor v) {
        set(index(c, r), v);
    }

    /** Set square with linearized index SQ to V.  This operation is
     *  undoable. */
    public void set(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /**Style tbh.
     * @param sqfrom from.
     * @param sqto to.
     * @return boolean**/
    boolean isAllowedMoveIndex(int sqfrom, int sqto) {
        int difference = Math.abs(sqfrom - sqto);
        int abc = EXTENDED_SIDE * 2 + 2;
        int bcd = EXTENDED_SIDE * 2 - 2;
        return (difference <= abc && difference >= bcd)
                || (difference <= 13 && difference >= 9)
                || (difference <= 2 && difference > 0);
    }


    /** Return true iff MOVE is legal on the current board. */
    boolean legalMove(Move move) {
        if (move == null) {
            return false;
        } else if (move == Move.PASS) {
            return (!canMove(whoseMove()));
        } else {
            return (get(move.fromIndex()) == whoseMove())
                    && (get(move.toIndex()) == EMPTY)
                    && (isAllowedMoveIndex(move.toIndex(),
                            move.fromIndex()));
        }
    }

    /** Return true iff player WHO can move, ignoring whether it is
     *  that player's move and whether the game is over. */
    boolean canMove(PieceColor who) {
        LinkedList<Character> col = new LinkedList<>();
        LinkedList<Character> row = new LinkedList<>();
        for (char l = "a".charAt(0); l <= "g".charAt(0); l++) {
            for (int in = 1; in <= 7; in++) {
                if (get(l, Integer.toString(in).charAt(0)) == who) {
                    col.add(l);
                    row.add(Integer.toString(in).charAt(0));
                }
            }
        }
        int in = 0;
        boolean track = false;
        while (in < col.size()) {
            if (canMoveHelper(col.get(in), row.get(in))) {
                return true;
            }
            in++;
        }
        return track;
    }

    /**Style tbh.
     * @param c c
     * @param r r
     * @return wagt**/
    boolean canMoveHelper(char c, char r) {
        String finder = "abcdefg";
        char start = c;
        char end = c;
        for (int in = 0; in < 7; in++) {
            if (finder.charAt(in) == c) {

                if (in == 0) {
                    start = "a".charAt(0);
                    end = "c".charAt(0);
                } else if (in == 1) {
                    start =  "a".charAt(0);
                    end = "d".charAt(0);
                } else if (in == 6) {
                    start = "e".charAt(0);
                    end = "g".charAt(0);
                } else if (in == 5) {
                    start = "d".charAt(0);
                    end = "g".charAt(0);
                } else {
                    start = finder.charAt(in - 2);
                    end = finder.charAt(in + 2);
                }
            }
        }
        int rowy = Character.getNumericValue(r);
        int startR = rowy - 2;
        int endR = rowy + 2;
        if (startR < 0) {
            startR = 0;
        }
        if (endR > 7) {
            endR = 7;
        }
        for (char ch = start; ch <= end; ch++) {
            for (int in = startR; in <= endR; in++) {
                if (get(ch, Integer.toString(in).charAt(0)) == EMPTY) {
                    return true;
                }
            }
        }
        return false;
    }

    /**Style tbh.
     * @param sq sq
     * @return wat**/
    ArrayList<Integer> availableExtendNeighbours(int sq) {
        ArrayList<Integer> result = new ArrayList<>();
        int[] inner = new int[] {neighbor(sq, -1, 0), neighbor(sq, -1, -1),
                neighbor(sq, -1, 1), neighbor(sq, 0, -1),
                neighbor(sq, 0, 1), neighbor(sq, 1, -1),
                neighbor(sq, 1, 0), neighbor(sq, 1, 1)};
        for (int i : inner) {
            if (get(i) == EMPTY) {
                result.add(i);
            }
        }
        return result;
    }

    /**Style tbh.
     * @param sq sq
     * @return goddream**/
    ArrayList<Integer> availableJumpNeighbours(int sq) {
        ArrayList<Integer> result = new ArrayList<>();
        int[] outter = new int[]{neighbor(sq, -2, -2), neighbor(sq, -2, -1),
                neighbor(sq, -2, 0), neighbor(sq, -2, 1), neighbor(sq, -2, 2),
                neighbor(sq, -1, -2), neighbor(sq, -1, 2), neighbor(sq, 0, -2),
                neighbor(sq, 0, 2), neighbor(sq, 1, -2), neighbor(sq, 1, 2),
                neighbor(sq, 2, -2), neighbor(sq, 2, -1), neighbor(sq, 2, 0),
                neighbor(sq, 2, 1), neighbor(sq, 2, 2)};
        for (int i : outter) {
            if (get(i) == EMPTY) {
                result.add(i);
            }
        }
        return result;
    }

    /**Style tbh.
     * @return arr
     * @param color colll**/
    ArrayList<Integer> piecesOfColor(PieceColor color) {
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < _board.length; i++) {
            if (get(i) == color) {
                result.add(i);
            }
        }
        return result;
    }


    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if gameOver(). */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Return total number of moves and passes since the last
     *  clear or the creation of the board. */
    int numMoves() {
        return _numMoves;
    }

    /** Return number of non-pass moves made in the current game since the
     *  last extend move added a piece to the board (or since the
     *  start of the game). Used to detect end-of-game. */
    int numJumps() {
        return _numJumps;
    }

    /** Changes all the neighbours of a
     * piece in place SQ to that piece's color. **/
    void changeNeighbors(int sq) {
        int[] interest = new int[] {
                neighbor(sq, -1, -1), neighbor(sq, -1, 0), neighbor(sq, -1, 1),
                        neighbor(sq, 0, -1), neighbor(sq, 0, 1),
                        neighbor(sq, 1, -1),
                        neighbor(sq, 1, 0), neighbor(sq, 1, 1)};
        ArrayList<Integer> changed = new ArrayList<>();
        for (int i: interest) {
            if (_board[i] != BLOCKED
                    && _board[i] != EMPTY
                    && _board[i] != whoseMove()) {
                _board[i] = whoseMove();
                changed.add(i);
                incrPieces(whoseMove(), 1);
                incrPieces(whoseMove().opposite(), -1);
            }
        }
        _changedColorArrays.add(changed);
    }

    /** Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     *  other than pass, assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        if (c0 == '-') {
            makeMove(Move.pass());
        } else {
            makeMove(Move.move(c0, r0, c1, r1));
        }
        _numMoves++;
    }

    /** Make the MOVE on this Board, assuming it is legal. */
    void makeMove(Move move) {
        assert legalMove(move);
        if (move.isPass()) {
            pass();
            return;
        }
        set(move.toIndex(), whoseMove());
        if (!move.isExtend()) {
            _numJumps++;
            set(move.fromIndex(), EMPTY);
        } else {
            _savedNumJumps.add(_numJumps);
            _numJumps = 0;
            incrPieces(whoseMove(), 1);
        }
        changeNeighbors(move.toIndex());
        _whoseMove = _whoseMove.opposite();
        _allMoves.add(move);
        _numMoves++;
        setChanged();
        notifyObservers();
    }

    /** Update to indicate that the current player passes, assuming it
     *  is legal to do so.  The only effect is to change whoseMove(). */
    void pass() {
        assert !canMove(_whoseMove);
        _whoseMove = _whoseMove.opposite();
        setChanged();
        notifyObservers();
    }

    /** Undo the last move. */
    void undo() {
        if (_allMoves.size() == 0) {
            return;
        }
        Move lastMove = _allMoves.pop();
        doReverseOf(lastMove);
        ArrayList<Integer> toBeChanged = _changedColorArrays.pop();
        for (int i = 0; i < toBeChanged.size(); i++) {
            PieceColor changebackInto = get(toBeChanged.get(i)).opposite();
            set(toBeChanged.get(i), changebackInto);
            incrPieces(changebackInto, 1);
            incrPieces(changebackInto.opposite(), -1);
        }
        _whoseMove = _whoseMove.opposite();
        setChanged();
        notifyObservers();
    }

    /**Style tbh.
     * @param move move **/
    void doReverseOf(Move move) {
        if (move.isExtend()) {
            PieceColor interest = get(move.col1(), move.row1());
            set(move.col1(), move.row1(), EMPTY);
            if (_savedNumJumps.size() != 0) {
                _numJumps = _savedNumJumps.pop();
            }
            incrPieces(interest, -1);
        } else if (move.isJump()) {
            set(move.col0(), move.row0(), whoseMove().opposite());
            set(move.col1(), move.row1(), EMPTY);
            _numJumps--;
        }
        _numMoves--;
    }

    /** Return true iff it is legal to place a block at C R. */
    boolean legalBlock(char c, char r) {
        if (c == "a".charAt(0) || c == "g".charAt(0)) {
            if (r == "1".charAt(0) || r == "7".charAt(0)) {
                return false;
            }
        }
        String finder = "abcdefg";
        int[] interest = new int[] {
                index(c, r),
                index(c, Integer.toString("8".charAt(0) - r).charAt(0)),
                index(finder.charAt("h".charAt(0) - c - 1), r),
                index(finder.charAt("h".charAt(0) - c - 1),
                        Integer.toString("8".charAt(0) - r).charAt(0))};
        for (int i: interest) {
            if (_board[i] != EMPTY) {
                return false;
            }
        }
        return true;
    }

    /** Return true iff it is legal to place a block at CR. */
    boolean legalBlock(String cr) {
        return legalBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Set a block on the square C R and its reflections across the middle
     *  row and/or column, if that square is unoccupied and not
     *  in one of the corners. Has no effect if any of the squares is
     *  already occupied by a block.  It is an error to place a block on a
     *  piece. */
    void setBlock(char c, char r) {
        if (!legalBlock(c, r)) {
            throw error("illegal block placement");
        }
        String finder = "abcdefg";
        set(c, r, BLOCKED);
        set(c, Integer.toString("8".charAt(0) - r).charAt(0), BLOCKED);
        set(finder.charAt("h".charAt(0) - c - 1), r, BLOCKED);
        set(finder.charAt("h".charAt(0) - c - 1),
                Integer.toString("8".charAt(0) - r).charAt(0), BLOCKED);
        setChanged();
        notifyObservers();
    }

    /** Place a block at CR. */
    void setBlock(String cr) {
        setBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Return a list of all moves made since the last clear (or start of
     *  game). */
    Stack<Move> allMoves() {
        return _allMoves;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /* .equals used only for testing purposes. */
    @Override
    public boolean equals(Object obj) {
        Board other = (Board) obj;
        return Arrays.equals(_board, other._board);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_board);
    }

    /**Style tbh.
     * @param color color
     * @return ret**/
    public String charVal(PieceColor color) {
        if (color == BLUE) {
            return "b";
        } else if (color == RED) {
            return "r";
        } else if (color == BLOCKED) {
            return "X";
        } else {
            return "-";
        }
    }

    /** Return a text depiction of the board (not a dump).  If LEGEND,
     *  supply row and column numbers around the edges. */
    String toString(boolean legend) {
        String everything = "===\n";
        if (legend) {
            for (int i = 7; i > 0; i--) {
                String line = Integer.toString(i) + " ";
                for (char a = "a".charAt(0); a <= "g".charAt(0); a++) {
                    if (a == "g".charAt(0)) {
                        line += charVal(get(a, Integer.toString(i).charAt(0)));
                    } else {
                        line += charVal(get(a,
                                Integer.toString(i).charAt(0))) + " ";
                    }
                }
                everything += "  " + line + "\n";
            }
            everything += "    a b c d e f g" + "\n" + "===";
            return everything;
        } else {
            for (int i = 7; i > 0; i--) {
                String line = "";
                for (char a = "a".charAt(0); a <= "g".charAt(0); a++) {
                    if (a == "g".charAt(0)) {
                        line += charVal(get(a,
                                Integer.toString(i).charAt(0)));
                    } else {
                        line += charVal(get(a,
                                Integer.toString(i).charAt(0))) + " ";
                    }
                }
                everything += "  " + line + "\n";
            }
            return everything + "===";
        }
    }

}
