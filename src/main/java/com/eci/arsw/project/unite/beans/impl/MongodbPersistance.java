package com.eci.arsw.project.unite.beans.impl;

import com.eci.arsw.project.unite.beans.UnitePersitence;
import com.eci.arsw.project.unite.model.*;
import com.eci.arsw.project.unite.repository.EventRepository;
import com.eci.arsw.project.unite.repository.EventsByUserRepository;
import com.eci.arsw.project.unite.repository.EventsInvitedByUserRepository;
import com.eci.arsw.project.unite.repository.UsersRepository;
import com.eci.arsw.project.unite.services.UniteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author SergioRt
 */

@Service
public class MongodbPersistance implements UnitePersitence {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private EventsByUserRepository eventsByUserRepository;

    @Autowired
    private EventsInvitedByUserRepository eventsInvitedByUserRepository;

    private Integer eventCounter;

    @Override
    public void createAccount(User user) throws UniteException {
        boolean exists = usersRepository.existsById(user.getUsername());

        if (!exists) {
            usersRepository.save(user);
        } else {
            throw new UniteException("Username is already taken");
        }
    }

    @Override
    public int createEvent(Event event) throws UniteException {
        eventCounter = getCounter();
        event.setId(eventCounter++);
        String owner = event.getOwner();
        event.getAssistantsState().put(owner,User.ASSISTANT);
        eventRepository.save(event);

        Optional<EventsByUser> events = eventsByUserRepository.findById(owner);
        if (events.isPresent()) {
            EventsByUser eventsByUser = events.get();
            eventsByUser.getEvents().add(event.getId());
            eventsByUserRepository.save(eventsByUser);

        } else {
            List<Integer> eventList;
            eventList = new CopyOnWriteArrayList<>();
            eventList.add(event.getId());
            eventsByUserRepository.save(new EventsByUser(owner, eventList));
        }

        inviteToEventOne(owner,event.getId());

        return event.getId();
    }

    @Override
    public List<Event> getEvents() throws UniteException {
        return eventRepository.findAll();
    }

    @Override
    public Event getEvent(int id) throws UniteException {
        Optional<Event> event = eventRepository.findById(id);
        if (event.isPresent()) {
            return event.get();
        } else {
            throw new UniteException(String.format("Event with id: %d not found.", id));
        }

    }

    @Override
    public List<Event> getEventsByUser(String username) throws UniteException {
        Optional<EventsByUser> eventsByUser = eventsByUserRepository.findById(username);
        if (eventsByUser.isPresent()) {
            List<Integer> ids = eventsByUser.get().getEvents();
            List<Event> events = new CopyOnWriteArrayList<>();
            for(Integer id: ids)
                events.add(getEvent(id));
            return events;
        } else {
            if (usersRepository.findById(username).isPresent())
                return new CopyOnWriteArrayList<>();
            else throw new UniteException("No found user " + username);
        }
    }

    @Override
    public User getUser(String username) throws UniteException {
        Optional<User> user = usersRepository.findById(username);
        if (user.isPresent()) {
            return user.get();
        } else {
            throw new UniteException("No found user " + username);
        }
    }

    @Override
    public User getUserByMail(String mail) throws UniteException {
        User user = usersRepository.findByMail(mail);
        if (user == null) {
            throw new UniteException("User with given mail does not exist");
        } else {
            return user;
        }
    }

    @Override
    public void changeEventName(int id, String name) throws UniteException {
        Event event = this.getEvent(id);
        event.setName(name);
        eventRepository.save(event);
    }

    @Override
    public void updateUser(String username, User user) throws UniteException {
        boolean exists = usersRepository.existsById(username);
        if (exists) {
            usersRepository.save(user);
        } else {
            throw new UniteException("No found user " + username);
        }
    }

    @Override
    public boolean checkUserAndPwd(String username, String pwd) throws UniteException {
        Optional<User> user = usersRepository.findById(username);
        if (user.isPresent()) {
            return user.get().getPassword().equals(pwd);
        } else {
            throw new UniteException("User with given username does not exist");
        }
    }

    @Override
    public Collection<String> getAllUsers() {
        List<User> users = usersRepository.findAll();
        List<String> usernames = new ArrayList<>();
        for (User u : users) {
            usernames.add(u.getUsername());
        }
        return usernames;
    }

    @Override
    public void saveMessage(int eventId, Message message) throws UniteException {
        Event event = getEvent(eventId);
        event.getChat().saveMessage(message);
        eventRepository.save(event);
    }

    @Override
    public List<Message> getMessagesByEvent(int eventId) throws UniteException {
        return getEvent(eventId).getChat().getRecord();
    }

    @Override
    public void saveLink(int eventId, Message message) throws UniteException {
        Event event = getEvent(eventId);
        event.getLinkChat().saveMessage(message);
        eventRepository.save(event);
    }

    @Override
    public List<Message> getLinkByEvent(int eventId) throws UniteException {
        return getEvent(eventId).getLinkChat().getRecord();
    }

    @Override
    public List<Event> getEventsInvitedByUser(String username) throws UniteException {
        Optional<EventsInvitedByUser> EventsInvitedByUser = eventsInvitedByUserRepository.findById(username);
        if (EventsInvitedByUser.isPresent()) {
            List<Integer> ids = EventsInvitedByUser.get().getEvents();
            List<Event> events = new CopyOnWriteArrayList<>();
            for(Integer id: ids)
                events.add(getEvent(id));
            return events;
        } else {
            throw new UniteException("The user is not invited to any event");
        }
    }

    @Override
    public Map<String,String> getAssistanceToEvent(int eventId) throws UniteException {
        return getEvent(eventId).getAssistantsState();
    }

    @Override
    public void changeStateOfAssitance(int eventId, String username, String state) throws UniteException {
        Event event = getEvent(eventId);
        event.changeStateOfUser(username, state);
        eventRepository.save(event);
    }

    @Override
    public void changePassword(String username, String newPassword) throws UniteException {
        User user = getUser(username);
        user.setPassword(newPassword);
        usersRepository.save(user);
    }

    @Override
    public void saveEventLocation(int eventId, String longitude, String latitude) throws UniteException {
        Event event = getEvent(eventId);
        event.setLocation("lon: " + longitude + " lat: " + latitude);
        eventRepository.save(event);
    }

    @Override
    public void inviteToEvent(int eventId, List<String> usernames) throws UniteException {
        Event event = getEvent(eventId);
        for(String username : usernames) {
            event.addMember(username, User.PENDING);
            inviteToEventOne(username,eventId);
        }
        eventRepository.save(event);


    }

    @Override
    public void inviteToEventOne(String username, int eventId){
        Optional<EventsInvitedByUser> events = eventsInvitedByUserRepository.findById(username);
        if (events.isPresent()) {
            EventsInvitedByUser eventsInvitedByUser = events.get();
            eventsInvitedByUser.getEvents().add(eventId);
            eventsInvitedByUserRepository.save(eventsInvitedByUser);

        } else {
            List<Integer> eventList;
            eventList = new CopyOnWriteArrayList<>();
            eventList.add(eventId);
            eventsInvitedByUserRepository.save(new EventsInvitedByUser(username, eventList));
        }
    }

    @Override
    public ItemSet getGatherOfEvent(int eventId) throws UniteException {
        return getEvent(eventId).getGather();
    }

    @Override
    public void addItem(int eventId, Item item) throws UniteException {
        Event event = getEvent(eventId);
        event.getGather().addItem(item);
        eventRepository.save(event);
    }

    @Override
    public void removeItem(int eventId, Item item) throws UniteException {
        Event event = getEvent(eventId);
        event.getGather().removeItem(item);
        eventRepository.save(event);
    }

    @Override
    public Poll getPollOfEvent(int eventId) throws UniteException {
        return getEvent(eventId).getPoll();
    }

    @Override
    public void takeChargeItem(int eventId, Item item) throws UniteException {
        Event event = getEvent(eventId);
        event.getGather().changeState(item);
        eventRepository.save(event);
    }

    @Override
    public void addTopicToEvent(int eventId, Topic topic) throws UniteException {
        Event event = getEvent(eventId);
        event.getPoll().addTopic(topic);
        eventRepository.save(event);
    }

    @Override
    public void removeTopicToEvent(int eventId, Topic topic) throws UniteException {
        Event event = getEvent(eventId);
        event.getPoll().removeTopic(topic);
        eventRepository.save(event);
    }

    @Override
    public Topic voteForTopicInEvent(int eventId, String username, Topic topic) throws UniteException {
        Event event = getEvent(eventId);
        Topic votedTopic = event.getPoll().vote(username, topic);
        eventRepository.save(event);
        return votedTopic;
    }

    @Override
    public void addItemChecklist(int eventId, Item item) throws UniteException {
        Event event = getEvent(eventId);
        event.getChecklist().addItem(item);
        eventRepository.save(event);
    }

    @Override
    public void removeItemChecklist(int eventId, Item item) throws UniteException {
        Event event = getEvent(eventId);
        event.getChecklist().removeItem(item);
        eventRepository.save(event);
    }

    @Override
    public void takeChargeItemChecklist(int eventId, Item item) throws UniteException {
        Event event = getEvent(eventId);
        event.getChecklist().changeState(item);
        eventRepository.save(event);
    }

    @Override
    public void changeDescription(int eventId, String newDescription) throws UniteException {
        Event event = getEvent(eventId);
        event.setDescription(newDescription);
        eventRepository.save(event);
    }

    @Override
    public void deleteEvent(int eventId) throws UniteException {
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if(eventOptional.isPresent()){
            Event event = eventOptional.get();
            eventRepository.deleteById(eventId);

            EventsByUser eventsByUser = eventsByUserRepository.findById(event.getOwner()).get();
            eventsByUser.getEvents().remove((Integer) eventId);
            eventsByUserRepository.save(eventsByUser);

            List<EventsInvitedByUser> eventsInvitedByUsers = eventsInvitedByUserRepository.findByEvents(eventId);
            for(int i = 0; i < eventsInvitedByUsers.size();i++){
                eventsInvitedByUsers.get(i).getEvents().remove((Integer) eventId);
            }
            eventsInvitedByUserRepository.saveAll(eventsInvitedByUsers);

        }else throw new UniteException("Event no exist");

    }

    private int getCounter() {
        Event event = eventRepository.findTopByOrderByIdDesc();
        int counter = 0;
        if(event != null){
            counter = event.getId() + 1;
        }
        return counter;
    }
    
     @Override
    public void updateWall(int eventId, String wall) throws UniteException {
        Event event = getEvent(eventId);
        event.setWall(wall);
        eventRepository.save(event);
    }

    @Override
    public ItemSet getChecklistOfEvent(int eventId) throws UniteException {
        Event event = getEvent(eventId);
        return event.getChecklist();
    }


}
