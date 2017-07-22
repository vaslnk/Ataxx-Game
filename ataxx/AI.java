package ataxx;

import java.util.ArrayList;
import java.util.Random;

import static ataxx.PieceColor.*;

/** A Player that computes its own moves.
 *  @author Yevgen Vasylenko
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 3;
    /** A position magnitude indicating a win (for red if positive, blue
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        PieceColor original = myColor();
        String color;
        if (myColor() == BLUE) {
            color = "Blue";
        } else {
            color = "Red";
        }
        Move move = findMove();
        if (move == Move.PASS || !board().canMove(original)) {
            System.out.println(color + " passes.");
            move = Move.PASS;
        } else {
            System.out.println(color + " moves " + move.toString() + ".");
        }
        return move;
    }

    /**Style tbh. @return move . **/
    private Move dumbMove() {
        ArrayList<Integer> thisColor = game().board().piecesOfColor(myColor());
        Random rand = new Random();
        int fromSQ = thisColor.get(rand.nextInt(thisColor.size()));
        while (!board().canMoveHelper(game().board().sqtoCol(fromSQ),
                game().board().sqtoRow(fromSQ))) {
            fromSQ = thisColor.get(rand.nextInt(thisColor.size()));
        }
        ArrayList<Integer> extendio =
                game().board().availableExtendNeighbours(fromSQ);
        ArrayList<Integer> jumpio =
                game().board().availableJumpNeighbours(fromSQ);
        int toSQ;
        if (extendio.size() == 0) {
            toSQ = jumpio.get(rand.nextInt(jumpio.size()));
        } else {
            toSQ = extendio.get(rand.nextInt(extendio.size()));
        }
        return Move.move(game().board().sqtoCol(fromSQ),
                game().board().sqtoRow(fromSQ),
                game().board().sqtoCol(toSQ),
                game().board().sqtoRow(toSQ));
    }

    /**Style tbh @return move .**/
    private Move stupidMove() {
        Board b = new Board(board());
        return simpleFindMax(b, -INFTY, INFTY);
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == RED) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** Used to communicate best moves found by findMove, when asked for. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value >= BETA if SENSE==1,
     *  and minimal value or value <= ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels before using a static estimate. */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        if (saveMove) {
            _lastFoundMove = findMax(board, depth, alpha, beta);
        }
        return 0;
    }

    /**Style tbh.
     * @param alpha a.
     * @param beta b.
     * @param board bf.
     * @return Move .**/
    private Move simpleFindMax(Board board, int alpha, int beta) {
        ArrayList<Move> chooseFrom = new ArrayList<>();
        Random rand = new Random();
        chooseFrom.add(Move.pass());
        ArrayList<Integer> thisColor = board.piecesOfColor(board.whoseMove());
        for (int fromSQ: thisColor) {
            ArrayList<Integer> availableMoveSQs = new ArrayList<>();
            availableMoveSQs.addAll(board.availableExtendNeighbours(fromSQ));
            availableMoveSQs.addAll(board.availableJumpNeighbours(fromSQ));
            for (int toSQ: availableMoveSQs) {
                Move attempt = Move.move(board.sqtoCol(fromSQ),
                        board.sqtoRow(fromSQ),
                        board.sqtoCol(toSQ), board.sqtoRow(toSQ));
                board.makeMove(attempt);
                if (board.gameOver()) {
                    board.undo();
                    return attempt;
                }
                if (board.numPieces(board.whoseMove().opposite()) > alpha) {
                    chooseFrom.clear();
                    chooseFrom.add(attempt);
                    alpha = board.numPieces(board.whoseMove().opposite());
                } else if (board.numPieces(board.whoseMove().opposite())
                        == alpha) {
                    chooseFrom.add(attempt);
                }
                board.undo();
            }
        }
        Move rslt = chooseFrom.get(rand.nextInt(chooseFrom.size()));
        while (chooseFrom.size() != 1 && rslt == Move.PASS) {
            rslt = chooseFrom.get(rand.nextInt(chooseFrom.size()));
        }
        return rslt;
    }

    /**Style tbh.
     * @param board b
     * @param depth d
     * @param alpha a
     * @param beta bbb
     * @return m.**/
    Move findMax(Board board, int depth, int alpha, int beta) {
        if (depth == 0 || board.numEmpty() >= 5) {
            return simpleFindMax(board, alpha, beta);
        }
        ArrayList<Move> chooseFrom = new ArrayList<>();
        ArrayList<Integer> thisColor = board.piecesOfColor(board.whoseMove());
        for (int fromSQ : thisColor) {
            ArrayList<Integer> availableMoveSQs = new ArrayList<>();
            availableMoveSQs.addAll(board.availableExtendNeighbours(fromSQ));
            availableMoveSQs.addAll(board.availableJumpNeighbours(fromSQ));
            for (int toSQ : availableMoveSQs) {
                Move movin = Move.move(board.sqtoCol(fromSQ),
                        board.sqtoRow(fromSQ),
                        board.sqtoCol(toSQ), board.sqtoRow(toSQ));
                if (!board.legalMove(movin)) {
                    board.changeWhoseMoveTo(myColor());
                }
                assert board.legalMove(movin);
                board.makeMove(movin);
                if (board.gameOver()) {
                    board.undo();
                    return movin;
                }
                setColor(myColor().opposite());
                Board b2 = new Board(board);
                Move response = findMax(b2, depth - 1, 0, beta);
                if (!board.legalMove(response)) {
                    response = simpleFindMax(board, -INFTY, INFTY);
                }
                assert board.legalMove(response);
                board.makeMove(response);
                setColor(myColor().opposite());
                if (board.numPieces(myColor()) > alpha) {
                    chooseFrom.clear();
                    chooseFrom.add(movin);
                    alpha = board.numPieces(myColor());
                } else if (board.numPieces(myColor()) == alpha) {
                    chooseFrom.add(movin);
                }
                board.undo();
                board.undo();
            }
        }
        Random rand = new Random();
        if (chooseFrom.size() == 0) {
            return stupidMove();
        }
        Move random = chooseFrom.get(rand.nextInt(chooseFrom.size()));
        return random;
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        return 0;
    }
}
