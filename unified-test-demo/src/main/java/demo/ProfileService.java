package demo;

import org.springframework.stereotype.Service;

@Service
public class ProfileService {
    public String save(String name, String email) {
        return "Saved " + name;
    }
}
