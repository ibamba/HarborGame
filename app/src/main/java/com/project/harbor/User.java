package com.project.harbor;

import java.io.Serializable;

class User implements Serializable {

    /**
     * User is a serailizable object that contains information about an user of the game
     * the pseudo, the motto, the max score and the name of the profile image of the user
     */
    private static final long serialVersionUID = 243226431580023681L;
    private String pseudo, motto;
    private int maxScore;
    private String uri;


    /**
     * For empty user. Default user
     */
    User() {
        this.pseudo = this.motto = "";
        this.maxScore = 0;
    }


    /**
     * Is default user?
     * @return true if user is default user
     */
    boolean isEmpty() {
        return pseudo.isEmpty() || motto.isEmpty();
    }

    //Setter and Getter
    String getPseudo() { return pseudo; }
    String getMotto() { return motto; }
    int getMaxScore() { return maxScore; }
    String getUri() { return uri; }

    void setPseudo(String pseudo) {
        this.pseudo = pseudo;
        if(this.getMotto().isEmpty()) this.motto = "Yor motto here !";
    }

    void setMotto(String motto) {
        if(!motto.isEmpty()) this.motto = motto;
    }

    void setMaxScore(int maxScore) { this.maxScore = maxScore; }
    void setUri(String uri) { this.uri = uri; }
}
