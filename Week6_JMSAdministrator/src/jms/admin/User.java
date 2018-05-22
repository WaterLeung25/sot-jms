package jms.admin;

public class User {
    private int userId;
    private int score;

    public User(int id, int score){
        setUserId(id);
        setScore(score);
    }

    public User(){}

    public int getUserId(){return userId;}

    public void setUserId(int id){
        this.userId = id;
    }

    public int getScore(){return score;}

    public void setScore(int score){
        this.score = score;
    }
}
