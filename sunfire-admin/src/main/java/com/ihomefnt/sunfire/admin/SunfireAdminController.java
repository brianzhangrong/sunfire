package com.ihomefnt.sunfire.admin;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SunfireAdminController {

    @RequestMapping(value = "/hi", method = {RequestMethod.GET, RequestMethod.POST})
    public String hello() {
        return "hi";
    }
}
