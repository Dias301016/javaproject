package kz.iitu.csse.group34.controllers;

import kz.iitu.csse.group34.entities.*;
import kz.iitu.csse.group34.repositories.CommentsRepository;
import kz.iitu.csse.group34.repositories.NewsPostRepository;
import kz.iitu.csse.group34.repositories.RolesRepository;
import kz.iitu.csse.group34.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;

@Controller
public class MainController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolesRepository rolesRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CommentsRepository commentsRepository;

    @Autowired
    private NewsPostRepository newsPostRepository;


    static List<String> list = new ArrayList<>();

    static{
        list.add("Positive");
        list.add("Negative");
        list.add("Neutral");
    }


    @GetMapping(value = "/")
    public String index(ModelMap model){


            List<NewsPost> items = newsPostRepository.findAll();
            model.addAttribute("itemler", items);
        if (getUserData()!= null) {
            if (getUserData().getIsActive() == null)
                return "redirect:/signout";
        }
        return "index";
    }

    @PreAuthorize("hasAnyRole('ROLE_MODERATOR')")
    @PostMapping("/deletePost{id}")
    public String deletepost(@PathVariable Long id){
        newsPostRepository.delete(newsPostRepository.findById(id).orElse(null));
        return "redirect:/";
    }

    @PostMapping("/deleteCom{id}")
        public String deltecom(@PathVariable Long id,@RequestParam Long post_id) {
            commentsRepository.delete(commentsRepository.findById(id).orElse(null));
            return "redirect:/details"+post_id;
    }


    @GetMapping("/details{id}")
    public String details(@PathVariable Long id,Model model){
        List<Comments> comments = commentsRepository.findAllByNewsPostId(id);
        comments.sort(Comparator.comparing(Comments::getPostDate));
        Collections.reverse(comments);
        model.addAttribute("item", newsPostRepository.findById(id).orElse(null));
        model.addAttribute("comments",comments );
        model.addAttribute("user", getUserData());
        Long aidi = 0L;
        if (getUserData()!=null)
            aidi = getUserData().getId();
        model.addAttribute("aidi",aidi);
        int neg=0,pos=0,neu=0,status;
        for (Comments com : comments) {
            if (com.getType() == 0) pos++;
            else if (com.getType() == 1) neg++;
            else neu++;
        }
        if (pos !=0 && neg!=0) {
            double np = (double) neg / pos;
            double pn = (double) neg / pos;
            if (np >= 2) {
                status = 1;
            } else if (np > 1 && np < 2) {
                status = 2;
            } else if (pn >= 2) {
                status = 5;
            } else if (pn > 1 && pn < 2) {
                status = 4;
            } else status = 3;
        }
        else {
            if (neg>pos)
                status=1;
            else if(pos>neg)
                status=5;
            else
                status=3;
        }
        if (neg==0 && pos ==0 && neu==0) status=0;
        System.out.println(status);
        model.addAttribute("status", status);
        model.addAttribute("types",list);
        return "details";
    }

    @PostMapping("/addUser")
    public String addUser(
                            @RequestParam String name,
                            @RequestParam String email, @RequestParam String pass,
                            @RequestParam(required = false) boolean moder, @RequestParam(required = false) boolean user
                          ){
        var roles = new HashSet<Roles>();
        if(moder)
            roles.add(rolesRepository.findById(2L).orElse(null));
        else
            roles.add(rolesRepository.findById(3L).orElse(null));
        userRepository.save(new Users(0L,email, passwordEncoder.encode(pass), name, true, roles));
        return "redirect:/users";
    }

    @PostMapping("/deleteUser")
    public String deleteuser() {
        Users u = getUserData();
        List<NewsPost> posts = newsPostRepository.findAllByAuthor(u);
        newsPostRepository.deleteAll(posts);
        userRepository.delete(u);
        return "redirect:/signout";
    }

    @PostMapping("/deleteUser{id}")
    public String deleteuse(@PathVariable Long id) {
        Users u = userRepository.findById(id).orElse(null);
        List<NewsPost> posts = newsPostRepository.findAllByAuthor(u);
        newsPostRepository.deleteAll(posts);
        userRepository.delete(u);
        return "redirect:/users";
    }

    @GetMapping("/editCom{id}")
    public String editcom(ModelMap map,@PathVariable Long id) {
        map.addAttribute("comment",commentsRepository.findById(id).orElse(null));
        return "editcom";
    }

    @PostMapping("/editCom")
    public String editcomm(@RequestParam String comment, @RequestParam String post_id, @RequestParam String com_id) {
        //String com = map.getAttribute("comment").toString();
        Comments com = commentsRepository.findById(Long.parseLong(com_id)).orElse(null);
        com.setComment(comment);
        commentsRepository.save(com);
        return "redirect:/details"+post_id;
    }

    @PostMapping("/editUser")
    public String edituser(@RequestParam String name, @RequestParam String password) {
        Users user = getUserData();
        user.setFullName(name);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        return "redirect:/profile";
    }

    @PostMapping("/updateUser{id}")
    public String updateuser(@PathVariable Long id, @RequestParam(required = false) Boolean moder) {
        Users u = userRepository.findById(id).orElse(null);
        Roles user = rolesRepository.findById(3L).orElse(null);
        Roles moderator = rolesRepository.findById(2L).orElse(null);
        Set<Roles> set = u.getRoles();
        if (moder != null) {
            set.remove(user);
            set.add(moderator);
        }
        else {
            set.remove(moderator);
            set.add(user);
        }
        u.setRoles(set);
        userRepository.save(u);
        return "redirect:/user"+id;
    }

    @PostMapping("/addComment{id}")
    public String addcomment(@RequestParam String comment, @PathVariable Long id, @RequestParam String review, Model model) {
        int rev = 0;
        if(review.equals("Positive")) {
            rev = 0;
        }
        else if(review.equals("Negative")){
            rev=1;
        }
        else if(review.equals("Neutral")){
            rev=2;
        }
        commentsRepository.save(new Comments(0L, getUserData(), newsPostRepository.findById(id).orElse(null),
                comment, new Date(), new HashSet<Users>(),rev));
        model.addAttribute("comments", commentsRepository.findAllByNewsPostId(id));
        return "redirect:/details"+id;
    }

    @GetMapping(path = "/login")
    public String loginPage(){
        return "login";
    }

    @GetMapping(path = "/profile")
    @PreAuthorize("isAuthenticated()")
    public String profilePage(Model model){
        model.addAttribute("user", getUserData());
        return "profile";

    }

    @PostMapping("/addPost")
    public String addPost(@RequestParam String title, @RequestParam String content, @RequestParam String shortContent, Model model){
        newsPostRepository.save(new NewsPost(0L,title,shortContent,content,getUserData(),new Date(),new HashSet<>()));
        return "redirect:/";
    }

    @PostMapping("/sort")
    public String filter(ModelMap model) {
        List<NewsPost> items = newsPostRepository.findAll();
        ArrayList<NewsPost> list = new ArrayList<>(items);
        Collections.sort(list);
        for (NewsPost n : list) {
            System.out.println(n.getPostDate());
        }
        model.addAttribute("items",list);
        return "redirect:/";
    }

    @PostMapping("/likeCom{id}")
    public String likeCom(@PathVariable Long id,@RequestParam Long post_id,ModelMap map) {
        Comments com = commentsRepository.findById(id).orElse(null);
        Users u = getUserData();
        com.getLikes().add(u);
        commentsRepository.save(com);
        return "redirect:/details"+post_id;
    }

    @PostMapping("/unlikeCom{id}")
    public String unlikeCom(@PathVariable Long id,@RequestParam Long post_id,ModelMap map) {
        Comments com = commentsRepository.findById(id).orElse(null);
        Users u = getUserData();
        com.getLikes().remove(u);
        commentsRepository.save(com);
        return "redirect:/details"+post_id;
    }

    @PostMapping("/likePost{id}")
    public String likePost(@PathVariable Long id) {
        NewsPost post = newsPostRepository.findById(id).orElse(null);
        post.getLikes().add(getUserData());
        newsPostRepository.save(post);
        return "redirect:/details"+id;
    }

    @PostMapping("/unlikePost{id}")
    public String unlikePost(@PathVariable Long id) {
        NewsPost post = newsPostRepository.findById(id).orElse(null);
        post.getLikes().remove(getUserData());
        newsPostRepository.save(post);
        return "redirect:/details"+id;
    }
    @GetMapping("/user{id}")
    public String getUser(@PathVariable Long id, Model model) {
        Users u = userRepository.findById(id).orElse(null);
        model.addAttribute("item", u);
        Roles r = null;
        for(Roles role : u.getRoles()) {
            r= role;
        }
        boolean bool=false;
        if (r.getRole().equals("ROLE_MODERATOR"))
            bool=true;
        model.addAttribute("bool", bool);
        return "user";
    }

    @PostMapping("/search")
    public String search(String search, Model model){
        HashSet<NewsPost> set = new HashSet<>();
        List<NewsPost> list = newsPostRepository.findAll();
        list.sort(Comparator.comparing(NewsPost::getPostDate));
        for(NewsPost n : list) {
            if(n.getContent().contains(search) || n.getShortContent().contains(search) || n.getTitle().contains(search)) {
                set.add(n);
            }
        }
        model.addAttribute("search",set);
        return "index";
    }

    @PostMapping("/order")
    public String order(){

        return null;
    }

    @GetMapping(path = "/users")
    public String usersPage(Model model){

        model.addAttribute("user", getUserData());

        List<Users> us = userRepository.findAll();
        List<Users> moders = new ArrayList<>();
        List<Users> users = new ArrayList<>();

        Roles moder=rolesRepository.findById(2L).orElse(null);

        Roles admin=rolesRepository.findById(1L).orElse(null);

        for (Users u : us)
                if(!u.getRoles().contains(admin) && !u.getRoles().contains(moder))
                    moders.add(u);

        for (Users u : us)
                if(!u.getRoles().contains(admin)) {
                    users.add(u);
                }

        model.addAttribute("userList", users);
        model.addAttribute("moders",moders);

        return "users";
    }

    @PostMapping("/register")
    String reg(@RequestParam String user_email, @RequestParam String user_password, @RequestParam String full_name, ModelMap map){

        if(userRepository.findByEmail(user_email).orElse(null)==null) {
            Users u = new Users();
            u.setEmail(user_email);
            u.setPassword(passwordEncoder.encode(user_password));
            u.setFullName(full_name);
            Roles r = rolesRepository.findById(3L).orElse(null);
            HashSet<Roles> roles = new HashSet<>();
            roles.add(r);
            u.setRoles(roles);
            userRepository.save(u);
            return "redirect:/login";
        }
        else {
            return "redirect:/register";
        }

    }

    @GetMapping("/register")
    String regi(){
        return "register";
    }

    public Users getUserData(){
        Users userData = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(!(authentication instanceof AnonymousAuthenticationToken)){
            User secUser = (User)authentication.getPrincipal();
            userData = userRepository.findByEmail(secUser.getUsername()).orElse(null);
        }
        return userData;
    }

}