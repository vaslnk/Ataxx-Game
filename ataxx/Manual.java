package ataxx;

import static ataxx.PieceColor.*;

/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author Yevgen Vasylenko
 */
class Manual extends Player {

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        String io;
        if (myColor() == BLUE) {
            io = "Blue: ";
        } else {
            io = "Red: ";
        }
        Command interest = game().getMoveCmnd(io);
        if (interest == null) {
            return null;
        }
        if (interest.commandType() == Command.Type.PASS) {
            return Move.pass();
        } else if (interest.commandType() == Command.Type.CLEAR) {
            game().doClear(interest.operands());
            return null;
        }
        String[] thisMove = interest.operands();
        char fromC = thisMove[0].charAt(0);
        char fromR = thisMove[1].charAt(0);
        char toC = thisMove[2].charAt(0);
        char toR = thisMove[3].charAt(0);
        return Move.move(fromC, fromR, toC, toR);
    }


}

