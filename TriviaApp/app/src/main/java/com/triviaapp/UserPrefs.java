package com.triviaapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class UserPrefs {

    private SharedPreferences sPrefs;
    private SharedPreferences.Editor editor;
    private final String isSessionStartedTag = "isSessionStarted", isButtonClickedTag = "isButtonClicked",
            isMessageSentTag = "isMessageSent", teamsTag = "teams", userTeamTag ="userTeam";


    public UserPrefs(Context context){
        sPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sPrefs.edit();
    }

    public void setIsSessionStarted(boolean state){
        editor.putBoolean(isSessionStartedTag, state);
        editor.apply();
    }
    public boolean getIsSessionStarted(){
        return sPrefs.getBoolean(isSessionStartedTag, false);
    }

    public void setIsMessageSent(boolean state){
        editor.putBoolean(isMessageSentTag, state);
        editor.apply();
    }
    public boolean getIsMessageSent(){
        return sPrefs.getBoolean(isMessageSentTag, false);
    }

    public void setIsButtonClicked(boolean state){
        editor.putBoolean(isMessageSentTag, state);
        editor.apply();
    }
    public boolean getIsButtonClicked(){
        return sPrefs.getBoolean(isMessageSentTag, false);
    }

    public void setTeams(String teams){
        editor.putString(teamsTag, teams);
        editor.apply();
    }
    public String getTeams(){
        return sPrefs.getString(teamsTag, null);
    }

    public void setUserTeam(String team){
        editor.putString(userTeamTag, team);
        editor.apply();
    }
    public String getUserTeam(){
        return sPrefs.getString(userTeamTag, null);
    }

}
