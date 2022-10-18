package com.example.springapi.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import javax.validation.ConstraintViolationException;
import com.example.springapi.apputil.AppUtils;
import com.example.springapi.dto.OrderDTO;
import com.example.springapi.dto.OrderWithProducts;
import com.example.springapi.dto.ProductDTO;
import com.example.springapi.mapper.MapperService;
import com.example.springapi.models.Cart;
import com.example.springapi.models.Comment;
import com.example.springapi.models.Discount;
import com.example.springapi.models.OrderDetail;
import com.example.springapi.models.OrderDetailKey;
import com.example.springapi.models.Orders;
import com.example.springapi.models.PKOfComment;
import com.example.springapi.models.Product;
import com.example.springapi.models.ResponseObject;
import com.example.springapi.repositories.CartResponsitory;
import com.example.springapi.repositories.CommentRepository;
import com.example.springapi.repositories.DiscountResponsitory;
import com.example.springapi.repositories.OrderDetailResponsitory;
import com.example.springapi.repositories.OrderResponsitory;
import com.example.springapi.repositories.ProductResponsitory;
import com.example.springapi.security.repository.UserRepository;
import com.example.springapi.service.QueryMySql;
import com.pusher.rest.Pusher;
import com.twilio.rest.media.v1.MediaProcessor.Order;

import lombok.Setter;

import com.example.springapi.security.entity.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/Orders")
public class OrderController {

    @Autowired
    public JavaMailSender emailSender;

    @Autowired
    ProductResponsitory productResponsitory;

    @Autowired
    OrderResponsitory orderResponsitory;

    @Autowired
    CartResponsitory cartResponsitory;

    @Autowired
    OrderDetailResponsitory orderDetailResponsitory;

    @Autowired
    UserRepository userResponsitory;

    @Autowired
    DiscountResponsitory discountResponsitory;

    @Autowired
    QueryMySql<OrderWithProducts> mysql;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    MapperService mapperService;

    @CrossOrigin(origins = "http://organicfood.com")
    @GetMapping("")
    List<Orders> getAllOrders() {
        return orderResponsitory.findAll();
    }

    @CrossOrigin(origins = "http://organicfood.com")
    @GetMapping("/sort")
    List<Orders> getAllOrdersAndSort(@RequestParam("type") String type) {
        if(type.equalsIgnoreCase("DESC")){
            return orderResponsitory.findByOrderByIdDesc();
        }else{
            return orderResponsitory.findByOrderByIdAsc();
        }
        
    }

    @GetMapping("/dto")
    List<OrderDTO> getAllOrdersDTO() {
        return mapperService.mapList(orderResponsitory.findAll(), OrderDTO.class);
    }

    @CrossOrigin(origins = "http://organicfood.com")
    @GetMapping("/OrderWithProducts/{id}")
    ResponseEntity<ResponseObject> getOrderWithProducts(@PathVariable("id") int id) {
        String sql = "select a.order_id id, c.name productName, quantity, price, discount "
                + "from (select order_id from orders where order_id=" + id + ") a, "
                + "(select order_order_id,product_product_id, quantity, price, discount from order_detail) b, "
                + "(select name, product_id from product) c\r\n"
                + "where a.order_id = b.order_order_id and b.product_product_id = c.product_id"
                + "order by a.order_id";
        return AppUtils.returnJS(HttpStatus.OK, "OK", "List product of order",
                mysql.select(OrderWithProducts.class.getName(), sql, null));
    }

    @CrossOrigin(origins = "http://organicfood.com")
    @GetMapping("user/{userId}")
    List<Orders> getAllOrdersByUserId(@PathVariable int userId) {
        return orderResponsitory.findAllByUserIdOrderByIdDesc(userId);
    }

    @CrossOrigin(origins = "http://organicfood.com")
    @GetMapping("/{id}")
    ResponseEntity<ResponseObject> getProduct(@PathVariable int id) {
        Optional<Orders> findOrder = orderResponsitory.findById(id);
        if (findOrder.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(
                    new ResponseObject("ok", "Query product sucessfully", findOrder));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ResponseObject("failed", "Can not find order with id=" + id, ""));
        }

    }

    // insert new Order with POST method
    // Postman : Raw, JSON
    // @PostMapping("/insert")
    // ResponseEntity<ResponseObject> insertOrder(@RequestBody Orders newOrders) {

    // return ResponseEntity.status(HttpStatus.OK).body(
    // new ResponseObject("ok", "Insert Order successfully",
    // orderResponsitory.save(newOrders))
    // );
    // }

    @CrossOrigin(origins = "http://organicfood.com")
    @PostMapping("/insert")
    ResponseEntity<ResponseObject> insertOrder(@RequestBody OrderDTO newOrderDTO) {
        Optional<User> user = userResponsitory.findById(newOrderDTO.getUserId());
        Optional<Discount> discount = null;
        Orders order;
        float km = 0;
        if (newOrderDTO.getDiscountId() != null) {
            discount = discountResponsitory.findById(newOrderDTO.getDiscountId());

            order = new Orders(
                    user.get(),
                    newOrderDTO.getCreateAt(),
                    discount.get(),
                    newOrderDTO.getState(),
                    newOrderDTO.getTotalPrice());
            km = discount.get().getPercent();

        } else {
            order = new Orders(
                    user.get(),
                    newOrderDTO.getCreateAt(),
                    null,
                    newOrderDTO.getState(),
                    newOrderDTO.getTotalPrice());
        }
        // Create a Simple MailMessage.
        SimpleMailMessage message = new SimpleMailMessage();

        Optional<Orders> optionalOrder = orderResponsitory.findTopByOrderByIdDesc();
        // String productName = "";
        // if(optionalOrder.isPresent()) {
        // List<OrderDetail> list = optionalOrder.get().getOrderDetails();
        // list.forEach(orderDetail ->{
        // if(productName.length()>0) {
        // productName+="\n";
        //
        // }
        // productName += orderDetail.getProduct().getName() + " 1x" +
        // orderDetail.getQuantity();
        // });
        // }
        int orderId = 100;
        if (optionalOrder.isPresent())
            orderId = optionalOrder.get().getId();
        if (user.get().getEmail() != null && !user.get().getEmail().equals("")) {
            message.setTo(user.get().getEmail());
            message.setSubject("Organic Food");
            message.setText("Thanks for your order\n" +
                    "Order information: \n" +
                    "Order ID: " + orderId + "\n" +
                    "Your name: " + order.getUser().getName() + "\n" +
                    "Date: " + new SimpleDateFormat("dd/MM/yyyy)").format(order.getCreateAt()) + "\n" +
                    "State: Waiting" + "\n");

            // Send Message!
            // this.emailSender.send(message);
        }
        // send noti
        // Pusher pusher = new Pusher("1415740", "747cc4a7b5556aa81191",
        // "97a46823bde563531c09");
        // pusher.setCluster("ap1");
        // pusher.setEncrypted(true);

        // pusher.trigger("my-channel", "my-event", Collections.singletonMap("message",
        // "hello world"));

        return ResponseEntity.status(HttpStatus.OK).body(
                new ResponseObject("ok", "Insert Order successfully", orderResponsitory.save(order)));
    }

    @CrossOrigin(origins = "http://organicfood.com")
    @PostMapping("/insert/v2")
    ResponseEntity<ResponseObject> insertOrderV2(@RequestBody OrderDTO newOrderDTO) {

        List<Cart> listCart = cartResponsitory.findAllByUserId(newOrderDTO.getUserId());
        if (listCart.size() == 0)
            return AppUtils.returnJS(HttpStatus.OK, "Failed", "No product in your cart", null);

        for (Cart cart : listCart) {
            if ( cart.getQuantity() > cart.getProduct().getTotal()){
                return ResponseEntity.status(HttpStatus.OK).body(
                new ResponseObject("Failed", "Số lượng không đủ. "+cart.getProduct().getName()+" còn "+cart.getProduct().getTotal()+" "+cart.getProduct().getCalculationUnit(), null));
            }
        }
        Optional<Discount> discount = null;
        Optional<User> user = userResponsitory.findById(newOrderDTO.getUserId());
        Orders order;
        float km = 0;
        if (newOrderDTO.getDiscountId() != null) {
            discount = discountResponsitory.findById(newOrderDTO.getDiscountId());

            order = new Orders(
                    user.get(),
                    newOrderDTO.getCreateAt(),
                    discount.get(),
                    newOrderDTO.getState(),
                    newOrderDTO.getTotalPrice());
            km = discount.get().getPercent();

        } else {
            order = new Orders(
                    user.get(),
                    newOrderDTO.getCreateAt(),
                    null,
                    newOrderDTO.getState(),
                    newOrderDTO.getTotalPrice());
        }
        // Create a Simple MailMessage.
        SimpleMailMessage message = new SimpleMailMessage();
        Orders ordered = null;
        try {
            ordered = orderResponsitory.save(order);
        } catch (Exception e) {
            return AppUtils.returnJS(HttpStatus.NOT_IMPLEMENTED, "Failed", "Insert order failed", null);
        }
        Optional<Orders> optionalOrder = orderResponsitory.findTopByOrderByIdDesc();
        // let insert order details follow cart of user
        int orderId = 100;
        String productName = "";
        if (optionalOrder.isPresent())
            orderId = optionalOrder.get().getId();
        for (Cart cart : listCart) {
            try {
                // if (productName.length() > 0) {
                //     productName += "\n";
                // } else {
                    productName += cart.getProduct().getName() + " x" + cart.getQuantity()+"\n";
                // }
                orderDetailResponsitory.save(new OrderDetail(
                        new OrderDetailKey(orderId, cart.getProduct().getProductId()),
                        optionalOrder.get(),
                        cart.getProduct(),
                        cart.getQuantity(),
                        cart.getProduct().getPrice(),
                        km+cart.getProduct().getDiscount()));
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        // String productName = "";
        // if(optionalOrder.isPresent()) {
        // List<OrderDetail> list = optionalOrder.get().getOrderDetails();
        // list.forEach(orderDetail ->{
        //
        //
        // }
        // productName += orderDetail.getProduct().getName() + " 1x" +
        // orderDetail.getQuantity();
        // });
        // }

        if (user.get().getEmail() != null && !user.get().getEmail().equals("")) {
            message.setTo(user.get().getEmail());
            message.setSubject("Organic Food");
            message.setText("Thanks for your order\n" +
                    "Order information: \n" +
                    "Order ID: " + orderId + "\n" +
                    "Your name: " + order.getUser().getName() + "\n" +
                    "Date: " + new SimpleDateFormat("dd/MM/yyyy)").format(order.getCreateAt()) + "\n" +
                    "State: Waiting" + "\n" +
                    "List of products:" + "\n" +
                    productName);

            // Send Message!
            this.emailSender.send(message);
        }

      
        // send noti
       // change quantity km
       if(km !=0){
        try {
            Discount discountTemp = ordered.getDiscount();
            int quantity = discountTemp.getQuantity() - 1;
            if(quantity <=  0)
            discountTemp.setQuantity(0);
            else discountTemp.setQuantity(quantity);
            discountResponsitory.save(discountTemp);
        } catch (Exception e) {
            //TODO: handle exception
            return ResponseEntity.status(HttpStatus.OK).body(
                new ResponseObject("Failed", "Update quantity discount failed", ordered));
        }
       }

       

       //change quantity product
       for (Cart cart : listCart) {
            Product productTemp = cart.getProduct();
            int quantity = productTemp.getTotal() - cart.getQuantity();
            productTemp.setTotal(quantity);
            productResponsitory.save(productTemp);
       }

        return ResponseEntity.status(HttpStatus.OK).body(
                new ResponseObject("Ok", "Insert Order successfully", ordered));
    }

    @CrossOrigin(origins = "http://organicfood.com")
    @PutMapping("/updateState/{id}")
    public ResponseEntity<ResponseObject> updateStateByOrderId(@PathVariable("id") int id,
            @RequestParam("state") String state) {
        Optional<Orders> optional = orderResponsitory.findById(id);
        if (optional.isPresent()) {
            Orders temp = optional.get();
            temp.setState(state);
          
            Orders orders = null;
            try {
                orders = orderResponsitory.save(temp);
            } catch (Exception e) {
                return AppUtils.returnJS(HttpStatus.OK, "Failed", "Update state order fail", e.getMessage());
            }
            if(state.equalsIgnoreCase("Đang giao")){
                Pusher pusher = new Pusher("1423362", "1988f25a6056e9b32057", "11020a120a866f41129c");
                pusher.setCluster("ap1");
                pusher.setEncrypted(true);
                HashMap<String, String> message = new HashMap<>();
                message.put("title", "Xác nhận đơn hàng");
                message.put("description", "Đơn hàng " + orders.getId() + " của bạn đã được duyệt");
                message.put("orderId", orders.getId() + "");
                message.put("userId", orders.getUser().getId() + "");
    
                pusher.trigger("my-channel", "my-event", message);
            }
            
            if(state.equalsIgnoreCase("Đã hủy")){
                for(OrderDetail orderDetail: temp.getOrderDetails()){
                    Product product = orderDetail.getProduct();
                    product.setTotal(product.getTotal() + orderDetail.getQuantity());
                    productResponsitory.save(product);
                }
                if(temp.getDiscount()!=null){
                    Discount discountTemp = discountResponsitory.getById(temp.getDiscount().getId()) ;
                    discountTemp.setQuantity(discountTemp.getQuantity() + 1);
                    discountResponsitory.save(discountTemp);
                }
                
            }
          
            return AppUtils.returnJS(HttpStatus.OK, "OK", "Update state order success", orders);
        } else {
            return AppUtils.returnJS(HttpStatus.NOT_FOUND, "Failed", "Not found this order", null);
        }
    }

    @CrossOrigin(origins = "http://organicfood.com")
    @GetMapping("/param/state")
    public ResponseEntity<ResponseObject> getOrdersByState(@RequestParam("state") String state) {
        List<Orders> list = new ArrayList<>();

        try {
            list = orderResponsitory.findAllByState(state);
            return AppUtils.returnJS(HttpStatus.OK, "OK", "Get order by state success ", list);
        } catch (ConstraintViolationException e) {
            // TODO: handle exception
            return AppUtils.returnJS(HttpStatus.OK, "OK", AppUtils.getExceptionSql(e), null);
        }

    }

    @CrossOrigin(origins = "http://organicfood.com")
    @GetMapping("/param/state/date")
    public ResponseEntity<ResponseObject> getOrdersByStateAndCreateAtBetween(@RequestParam("state") String state,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        List<Orders> list = new ArrayList<>();
        String patternDate = "dd-MM-yyyy";

        try {
            list = orderResponsitory.findAllByStateAndCreateAtBetweenOrderByIdDesc(state,
                    AppUtils.stringToDate(startDate, patternDate),
                    AppUtils.stringToDate(endDate, patternDate));
            return AppUtils.returnJS(HttpStatus.OK, "OK", "Get order by state success ", list);
        } catch (ConstraintViolationException e) {
            // TODO: handle exception
            return AppUtils.returnJS(HttpStatus.OK, "OK", AppUtils.getExceptionSql(e), null);
        }

    }


    @CrossOrigin(origins = "http://organicfood.com")
    @PostMapping("/{id}/comment/insert")
    public ResponseEntity<ResponseObject> getOrdersByStateAndCreateAtBetween(@PathVariable("id") int orderId,
            @RequestParam("userId") int userId,
            @RequestParam("rate") int rating,
            @RequestParam("comment") String comment) {
        Optional<Orders> optionalOrder = orderResponsitory.findById(orderId);
        if(!optionalOrder.isPresent()){
            return AppUtils.returnJS(HttpStatus.NOT_FOUND, "Failed", "Order not found", null);
        }

        optionalOrder.get().getOrderDetails().forEach(
            orderDetail -> {
                try {
                    commentRepository.save(new Comment("userId" + userId + "-orderId" +  orderId+ "-productId" +orderDetail.getProduct().getProductId() , new Date(), rating, comment,orderDetail.getProduct().getProductId(),userId));
                } catch (Exception e) {
                    //TODO: handle exception
                }
            }
        );

        // update commentd of orders
        Orders orders = optionalOrder.get();
        orders.setCommented(true);

        try {
            orders = orderResponsitory.save(orders);
            
        } catch (Exception e) {
            return AppUtils.returnJS(HttpStatus.NOT_IMPLEMENTED, "Failed", "Update commented of order failed", optionalOrder.get());
        }

        return AppUtils.returnJS(HttpStatus.OK, "OK", "Insert comment for order success", orders);

    }



}
