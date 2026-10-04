package com.dilton.paytm.controller;

import com.dilton.paytm.entity.Show;
import com.dilton.paytm.service.ShowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping
    public ResponseEntity<Show> createShow(@RequestBody CreateShowRequest request) {

        Show show = showService.createShow(
                request.name(),
                request.seats(),
                request.pricePaise()
        );

        return ResponseEntity.status(201).body(show);
    }

    public record CreateShowRequest(
            String name,
            List<String> seats,
            Long pricePaise
    ) {
    }
}