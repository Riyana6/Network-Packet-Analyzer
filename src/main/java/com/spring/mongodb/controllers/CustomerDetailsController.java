package com.spring.mongodb.controllers;

import com.spring.mongodb.models.CustomerDetails;
import com.spring.mongodb.payload.request.CustomerRequest;
import com.spring.mongodb.payload.response.MessageResponse;
import com.spring.mongodb.repository.CustomerRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;       
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")

public class CustomerDetailsController {


    @Autowired
    CustomerRequestRepository customerRequestRepository;
    
    //add request
    @PostMapping("/customer")
    public ResponseEntity<?> customerRequest(@Valid @RequestBody CustomerRequest customerRequest) {
        CustomerDetails request = new CustomerDetails(customerRequest.getEmail(),
                customerRequest.getC_name(),
                customerRequest.getC_nic(),
                customerRequest.getPhone(),
                customerRequest.getReason(),
                customerRequest.getDate()
                );

        customerRequestRepository.save(request);

        return ResponseEntity.ok(new MessageResponse("Customer request saved successfully!"));
    }

    //update customer request
    @PutMapping("/request/{id}")
    public ResponseEntity<CustomerDetails> updateReq (@RequestBody CustomerRequest customerRequest, @PathVariable String id){
        Optional<CustomerDetails> reqData = customerRequestRepository.findById(id);
        if (reqData.isPresent()) {
            System.out.println("reading customer request");
            CustomerDetails _req = reqData.get();

            _req.setC_name(customerRequest.getC_name());
            _req.setC_nic(customerRequest.getC_nic());
            _req.setPhone(customerRequest.getPhone());
            _req.setReason(customerRequest.getReason());
            _req.setDate(customerRequest.getDate());
            return new ResponseEntity<>(customerRequestRepository.save(_req), HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }



    //get requests
    @GetMapping("/requests")
    public List<CustomerDetails> getAllCustomerRequests() {
        System.out.println("Get all Customer Requests...");

        return customerRequestRepository.findAll();
    }

    //delete requests
    @DeleteMapping("/request/{id}")
    public ResponseEntity<HttpStatus> removeRequest(@PathVariable String id) {
        try {
            customerRequestRepository.deleteById(id);
            System.out.println("Deleted request of : "+ id);
            ResponseEntity.ok(new MessageResponse("request deleted!"));
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
