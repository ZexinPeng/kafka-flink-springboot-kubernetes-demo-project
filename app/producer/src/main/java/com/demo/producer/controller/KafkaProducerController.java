package com.demo.producer.controller;

import com.demo.producer.service.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class KafkaProducerController {

    @Autowired
    private KafkaProducer kafkaProducer;

    @GetMapping("/publish")
    public ResponseEntity<String> publish(){
//        kafkaProducer.sendMessage();
        return ResponseEntity.ok("Message sent to kafka topic");
    }

    @RequestMapping("/index")
    public String index(@RequestParam(value = "fruit", required = false) String fruit) {
        if (fruit != null && !fruit.isEmpty()) {
            kafkaProducer.sendMessage(fruit);
        }
        return "index";
    }
}
