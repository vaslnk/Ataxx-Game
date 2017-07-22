package ataxx;

import org.junit.Test;

import static org.junit.Assert.*;

/** Tests of the Board class.
 *  @author Yevgen Vasylenko
 */
public class BoardTest {

    private static final String[]
        GAME1 = {"a7-b7", "a1-a2", "a7-a6", "a2-a3", "a6-a5", "a3-a4"};
    private static final String[]
        TEST1 = {"a7-a6", "a1-b1", "a6-b5"};


    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(s.charAt(0), s.charAt(1),
                       s.charAt(3), s.charAt(4));
        }
    }

    @Test public void testUndo() {
        Board b0 = new Board();
        Board b1 = new Board(b0);
        makeMoves(b0, GAME1);
        Board b2 = new Board(b0);
        for (int i = 0; i < GAME1.length; i += 1) {
            b0.undo();
        }
        assertEquals("failed to return to start", b1, b0);
        makeMoves(b0, GAME1);
        assertEquals("second pass failed to reach same position", b2, b0);
    }

    @Test public void myTest() {
        Board b = new Board();
        b.setBlock("a".charAt(0), "3".charAt(0));
    }

    @Test public void ohNo() {
        Board b0 = new Board();
        char a = b0.sqtoCol(58);
        char b = b0.sqtoRow(58);
    }

}
