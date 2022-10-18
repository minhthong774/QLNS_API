package com.example.springapi.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springapi.apputil.AppUtils;
import com.example.springapi.dto.ProductDTO;
import com.example.springapi.models.Category;
import com.example.springapi.models.Orders;
import com.example.springapi.models.Product;
import com.example.springapi.models.ProductReport;
import com.example.springapi.models.ResponseObject;
import com.example.springapi.models.Total;
import com.example.springapi.repositories.CategoryResponsitory;
import com.example.springapi.repositories.OrderResponsitory;
import com.example.springapi.repositories.ProductResponsitory;
import com.example.springapi.repositories.ReportRepository;
import com.example.springapi.service.QueryMySql;
import com.twilio.rest.media.v1.MediaProcessor.Order;

@RestController
@RequestMapping(path = "/api/v1/Reports/")
public class ReportController {

	@Autowired
	ProductResponsitory productReponsitory;

	@Autowired
	QueryMySql<ProductReport> queryMySql;

	@Autowired
	OrderResponsitory orderResponsitory;
	@Autowired
	CategoryResponsitory categoryResponsitory;

	@CrossOrigin(origins = "http://organicfood.com")
	@GetMapping("ProductRevenue/param4")
	public ResponseEntity<ResponseObject> getProductRevenue(
			@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "endDate", required = false, defaultValue = "22-2-2222") String endDate,
			@RequestParam("limit") int limit,
			@RequestParam("offset") int offset) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			startDate = sdf.format(new SimpleDateFormat("dd-MM-yyyy").parse(startDate));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			endDate = sdf.format(new SimpleDateFormat("dd-MM-yyyy").parse(endDate));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String sql = "select product_product_id id,product.name, sum(b.doanhthu) revenue from (select  product_product_id, a.quantity, price,percent, price*a.quantity*(1-COALESCE(percent,0)) 							doanhthu\r\n"
				+ "from (select orders.order_id, product_product_id, order_detail.quantity, price, discount_id \r\n"
				+ "from orders,order_detail \r\n"
				+ "where orders.order_id= order_order_id and (create_at between '"
				+ startDate + "' and '" + endDate + "')) a\r\n"
				+ "left join discount on a.discount_id = discount.discount_id) b,\r\n"
				+ "product where product_id=product_product_id\r\n"
				+ "group by product_product_id, product.name\r\n"
				+ "order by revenue desc\r\n"
				+ "limit " + limit + " offset " + offset + "";
		List<ProductReport> list = queryMySql.select(ProductReport.class.getName(),
				sql, null);
		return AppUtils.returnJS(HttpStatus.OK, "OK", "Request product revenue success", list);
	}

	@CrossOrigin(origins = "http://organicfood.com")
	@GetMapping("total")
	public Total getTotal() {
		long totalProduct = productReponsitory.count();
		long totalCategory = categoryResponsitory.count();
		List<Orders> orders = orderResponsitory.findAllByState("Đã giao");
		// List<Orders> orders = orderResponsitory.findAllByState(AppUtils.orderState[2]);
		float total = 0;
		for (Orders orders2 : orders) {
			if (orders2.getDiscount() != null) {
				total += orders2.totalMountOfOrder() * (1 - orders2.getDiscount().getPercent());
			}

		}
		return new Total(totalProduct, totalCategory, orders.size(), total);
	}

}
