package map.cell;

import static com.google.common.base.Preconditions.*;

import map.content.Content;
import map.player.Player;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({"unused", "UnusedReturnValue", "WeakerAccess"})
public class Cell implements Serializable {
    private Set<Long> users;
    private Content content;
    private Cell level;
    private Cell right;
    private Cell down;
    private Cell left;
    private Cell up;

    private Cell() {

    }

    public static CellBuilder newBuilder() {
        return new Cell().new CellBuilder();
    }

    public boolean addUser(Player player) {
        return this.addUser(player.id());
    }

    public class CellBuilder {
        private CellBuilder() {

        }

        public CellBuilder setLevel(Cell level) {
            Cell.this.level = level;
            return this;
        }

        public CellBuilder setUp(Cell up) {
            Cell.this.up = up;
            return this;
        }

        public CellBuilder setDown(Cell down) {
            Cell.this.down = down;
            return this;
        }

        public CellBuilder setRight(Cell right) {
            Cell.this.right = right;
            return this;
        }

        public CellBuilder setLeft(Cell left) {
            Cell.this.left = left;
            return this;
        }

        public CellBuilder thisLevel() {
            return this.setLevel(Cell.this);
        }

        public CellBuilder nullDown() {
            return this.setDown(null);
        }

        public CellBuilder nullUp() {
            return this.setUp(null);
        }

        public CellBuilder nullLeft() {
            return this.setLeft(null);
        }

        public CellBuilder nullRight() {
            return this.setRight(null);
        }

        public CellBuilder emptyItem() {
            Cell.this.content = null;
            return this;
        }

        public CellBuilder setContent(Content content) {
            Cell.this.content = content;
            return this;
        }

        public CellBuilder noOneWas() {
            return this.setWhoWas(new HashSet<>());
        }

        public CellBuilder emptyCoordinates() {
            return this.nullUp().nullDown().nullLeft().nullRight();
        }

        public CellBuilder setWhoWas(Set<Long> users) {
            Cell.this.users = users;
            return this;
        }

        public Cell build() {
            checkArgument(Cell.this.level != null);
            checkArgument(Cell.this.users != null);
            return Cell.this;
        }
    }

    public Cell up() {
        return up;
    }

    public Cell down() {
        return down;
    }

    public Cell left() {
        return left;
    }

    public Cell right() {
        return right;
    }

    public Cell level() {
        return level;
    }

    public Cell downLeft() {
        return down == null ? null : down.left;
    }

    public Cell downRight() {
        return down == null ? null : down.right;
    }

    public Cell upLeft() {
        return up == null ? null : up.left;
    }

    public Cell upRight() {
        return up == null ? null : up.right;
    }

    public Content content() {
        assert content != null;
        return content;
    }

    public Cell setContent(Content content) {
        this.content = content;
        return this;
    }

    public Cell deleteContent() {
        return this.setContent(null);
    }

    public Cell setUp(Cell up) {
        this.up = up;
        return this;
    }

    public Cell setDown(Cell down) {
        this.down = down;
        return this;
    }

    public Cell setRight(Cell right) {
        this.right = right;
        return this;
    }

    public Cell setLeft(Cell left) {
        this.left = left;
        return this;
    }

    public boolean contains(long user) {
        return this.users.contains(user);
    }

    public boolean addUser(long user) {
        return this.users.add(user);
    }

    public boolean empty() {
        return content == null;
    }

    public boolean aroundAnyEquals(Cell cell) {
        return     up()        == cell
                || left()      == cell
                || right()     == cell
                || down()      == cell
                || downLeft()  == cell
                || downRight() == cell
                || upLeft()    == cell
                || upRight()   == cell;
    }
}
