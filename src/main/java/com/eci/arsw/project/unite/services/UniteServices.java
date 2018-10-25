package com.eci.arsw.project.unite.services;

import com.eci.arsw.project.unite.model.Event;
import com.eci.arsw.project.unite.model.User;
import java.util.List;
import java.util.Set;

/**
 *
 * @author sergio
 */
public interface UniteServices {

    List<Event> getEvents() throws UniteException;

    Event getEvent(int id) throws UniteException;

    List<Event> getEventsByUser(String username) throws UniteException;

    void createEvent(Event event) throws UniteException;

    void changeEventName(int id, String name) throws UniteException;

    void createAccount(User user)throws UniteException;    

    public void updateUser(String username, User user) throws UniteException;
    
    public boolean grantAccess(String username, String pwd) throws UniteException;
    
    public Set<String> getAllUsers();

    void createAccount(String username, String mail, String name, String password) throws UniteException;
    
    public void joinToEventByUsername(int id, String username) throws UniteException;
    
    public void joinToEventByMail(int id, String mail) throws UniteException;

}
