package com.zsr.mall.controller.force;

import com.alibaba.fastjson.JSONObject;
import com.zsr.mall.controller.BaseController;
import com.zsr.mall.entity.Product;
import com.zsr.mall.entity.ProductOrderItem;
import com.zsr.mall.entity.User;
import com.zsr.mall.service.*;
import com.zsr.mall.util.PageUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
public class ForeOrderController extends BaseController {
    @Resource
    private ProductService productService;
    @Resource
    private ProductOrderItemService productOrderItemService;
    @Resource
    private UserService userService;
    @Resource
    private ProductImageService productImageService;
    @Resource
    private CategoryService categoryService;

    //创建订单项-购物车-ajax
    @ResponseBody
    @RequestMapping(value = "orderItem/create/{productId}", method = RequestMethod.POST,produces = "application/json;charset=utf-8")
    public  String createOrderItem(@PathVariable("productId") Integer productId,
                                   @RequestParam(required = false,defaultValue = "1") Short productNumber,
                                   HttpSession session,
                                   HttpServletRequest request) {
        System.out.println("===========================================");
        JSONObject object = new JSONObject();
        logger.info("检查用户是否登录");
        Object userId = checkUser(session);
        if (userId == null) {
            object.put("url", "/login");
            object.put("success", false);
            return object.toJSONString();
        }

        logger.info("通过产品ID获取产品信息:{}", productId);
        Product product = productService.get(productId);
        if (product == null) {
            object.put("url", "/login");
            object.put("success", false);
            return object.toJSONString();
        }
        ProductOrderItem productOrderItem = new ProductOrderItem();
        logger.info("检查用户的购物车项");
        List<ProductOrderItem> orderItemList = productOrderItemService.getListByUserId(Integer.valueOf(userId.toString()),null);
        for (ProductOrderItem orderItem : orderItemList) {
            if (orderItem.getProductOrderItemProduct().getProductId().equals(productId)) {
                logger.info("找到已有的产品", "进行数量追加");
                int number = orderItem.getProductOrderItemNumber();
                number += 1;
                //获取购物车中原商品的购物车id，进行修改
                productOrderItem.setProductOrderItemId(orderItem.getProductOrderItemId());
                //把新的购物车商品数量修改到指定记录
                productOrderItem.setProductOrderItemNumber((short) number);
                //因为数量发生改变,所以总价也需要从新计算,并且修改到原记录中
                productOrderItem.setProductOrderItemPrice(number * product.getProductPrice());
                //开始修改购物车
                boolean yn = productOrderItemService.update(productOrderItem);
                if (yn) {
                    object.put("success", true);
                } else {
                    object.put("success", false);
                }
                return object.toJSONString();
            }
        }
        logger.info("封装订单项对象");
        productOrderItem.setProductOrderItemProduct(product);
        productOrderItem.setProductOrderItemNumber(productNumber);
        productOrderItem.setProductOrderItemPrice(product.getProductSalePrice() * productNumber);
        productOrderItem.setProductOrderItemUser(new User(Integer.valueOf(userId.toString())));
        boolean yn = productOrderItemService.add(productOrderItem);
        if (yn) {
            object.put("success", true);
        } else {
            object.put("success", false);
        }
        return object.toJSONString();
    }

    //转到前台Mall-购物车页
    @RequestMapping(value = "cart",method = RequestMethod.GET)
    public String goToCartPage(Map<String,Object> map,HttpSession session){
        logger.info("检查用户是否登录");
        Object userId=checkUser(session);
        User user;
        if(userId!=null){
            logger.info("获取用户信息");
            user=userService.get(Integer.parseInt(userId.toString()));
            map.put("user",user);
        }else {
            return "redirect:/login";
        }
        logger.info("获取用户购物车信息");
        List<ProductOrderItem> orderItemList=productOrderItemService.getListByUserId(Integer.valueOf(userId.toString()),null);
        Integer orderItemTotal=0;
        if(orderItemList.size()>0){
            logger.info("获取用户购物车的商品总数");
            orderItemTotal=productOrderItemService.getTotalByUserId(Integer.valueOf(userId.toString()));
            logger.info("获取用户购物车内的商品信息");
            for (ProductOrderItem orderItem:orderItemList){
                //获取购物车中每一个商品的id
                Integer productId=orderItem.getProductOrderItemProduct().getProductId();
                //通过商品id去查询商品的信息
                Product product=productService.get(productId);
                product.setSingleProductImageList(productImageService.getList(productId,(byte) 0,null));
                product.setProductCategory(categoryService.get(product.getProductCategory().getCategoryId()));
                orderItem.setProductOrderItemProduct(product);
            }
        }
        map.put("orderItemList",orderItemList);
        map.put("orderItemTotal",orderItemTotal);

        logger.info("转到前台Mall-购物车页");
        return "fore/productBuyCarPage";
    }



}
