package ataxx;

import org.junit.Test;

import static org.junit.Assert.*;

/** Tests of the undo function.
 *  @author Yevgen Vasylenko
 */
public class MyTest {

    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(s.charAt(0), s.charAt(1),
                    s.charAt(3), s.charAt(4));
        }
    }


    @Test public void hell() {
        Board b = new Board();
        int[] red = new int[] {24, 25, 27, 28, 35, 36, 37, 38, 39, 40,
            52, 74, 82, 83, 84, 85, 90, 91, 93, 94, 95, 96};
        int[] blue = new int[] {26, 29, 30, 41, 48, 49, 50, 51, 58, 59,
            60, 61, 62, 63, 68, 69, 70, 71, 72, 73, 79, 80, 81, 92};
        Board bC = new Board(b);
        for (int x: red) {
            b.set(x, PieceColor.RED);
        }
        for (int y: blue) {
            b.set(y, PieceColor.BLUE);
        }
        b.makeMove("a".charAt(0), "2".charAt(0), "a".charAt(0), "3".charAt(0));
        b.makeMove("c".charAt(0), "3".charAt(0), "b".charAt(0), "3".charAt(0));
        b.undo();
        b.undo();
        assertEquals("Wrong", bC, b);
    }

}
