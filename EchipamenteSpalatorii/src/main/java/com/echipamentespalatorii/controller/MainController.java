package com.echipamentespalatorii.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.echipamentespalatorii.dao.OrderDAO;
import com.echipamentespalatorii.dao.ProductDAO;
import com.echipamentespalatorii.entity.Product;
import com.echipamentespalatorii.form.CustomerForm;
import com.echipamentespalatorii.model.CartInfo;
import com.echipamentespalatorii.model.CustomerInfo;
import com.echipamentespalatorii.model.ProductInfo;
import com.echipamentespalatorii.pagination.PaginationResult;
import com.echipamentespalatorii.utils.Utils;
import com.echipamentespalatorii.validator.CustomerFormValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Transactional
public class MainController {
	
  @Autowired
  private ProductDAO productDAO;

  @Autowired
  private CustomerFormValidator customerFormValidator;
	
  @Autowired
  private OrderDAO orderDAO;

  @InitBinder
  public void myInitBinder(WebDataBinder dataBinder) {
     Object target = dataBinder.getTarget();
     if (target == null) {
        return;
     }
     System.out.println("Target=" + target);

     if (target.getClass() == CartInfo.class) {

     }

     else if (target.getClass() == CustomerForm.class) {
        dataBinder.setValidator(customerFormValidator);
     }

  }
  
  @RequestMapping("/404")
  public String notExist() {
     return "/404";
  }
  
  /*@RequestMapping("/error")
  public String basicErrorController() {
     return "/eroare";
  }*/

  @RequestMapping("/403")
  public String accessDenied() {
     return "/403";
  }
  
  // Pagina de acasa
  @RequestMapping("/")
  public String home() {
     return "index";
  }

  // Pagina produse
  @RequestMapping({ "/productList" })
  public String listProductHandler(Model model, //
        @RequestParam(value = "name", defaultValue = "") String likeName,
        @RequestParam(value = "page", defaultValue = "1") int page) {
     final int maxResult = 5;
     final int maxNavigationPage = 10;

     PaginationResult<ProductInfo> result = productDAO.queryProducts(page, //
           maxResult, maxNavigationPage, likeName);

     model.addAttribute("paginationProducts", result);
     return "productList";
  }

  @RequestMapping({ "/buyProduct" })
  public String listProductHandler(HttpServletRequest request, Model model, //
        @RequestParam(value = "code", defaultValue = "") String code) {

     Product product = null;
     if (code != null && code.length() > 0) {
        product = productDAO.findProduct(code);
     }
     if (product != null) {

        CartInfo cartInfo = Utils.getCartInSession(request);
        ProductInfo productInfo = new ProductInfo(product);
        cartInfo.addProduct(productInfo, 1);
     }

     return "redirect:/shoppingCart";
  }

  @RequestMapping({ "/shoppingCartRemoveProduct" })
  public String removeProductHandler(HttpServletRequest request, Model model, //
        @RequestParam(value = "code", defaultValue = "") String code) {
     Product product = null;
     if (code != null && code.length() > 0) {
        product = productDAO.findProduct(code);
     }
     if (product != null) {

        CartInfo cartInfo = Utils.getCartInSession(request);

        ProductInfo productInfo = new ProductInfo(product);

        cartInfo.removeProduct(productInfo);

     }

     return "redirect:/shoppingCart";
  }

  // POST: Actualizare cos
  @RequestMapping(value = { "/shoppingCart" }, method = RequestMethod.POST)
  public String shoppingCartUpdateQty(HttpServletRequest request, //
        Model model, //
        @ModelAttribute("cartForm") CartInfo cartForm) {

     CartInfo cartInfo = Utils.getCartInSession(request);
     cartInfo.updateQuantity(cartForm);

     return "redirect:/shoppingCart";
  }

  // GET: Pagina cos
  @RequestMapping(value = { "/shoppingCart" }, method = RequestMethod.GET)
  public String shoppingCartHandler(HttpServletRequest request, Model model) {
     CartInfo myCart = Utils.getCartInSession(request);

     model.addAttribute("cartForm", myCart);
     return "shoppingCart";
  }

  // GET: Adaugare informatii client
  @RequestMapping(value = { "/shoppingCartCustomer" }, method = RequestMethod.GET)
  public String shoppingCartCustomerForm(HttpServletRequest request, Model model) {

     CartInfo cartInfo = Utils.getCartInSession(request);
     
     

     if (cartInfo.isEmpty()) {

        return "redirect:/shoppingCart";
     }
     
     CustomerInfo customerInfo = cartInfo.getCustomerInfo();
     CustomerForm customerForm = new CustomerForm(customerInfo);
     model.addAttribute("customerForm", customerForm);
     
     return "shoppingCartCustomer";
  }

  // POST: Salvare informatii client
  @RequestMapping(value = { "/shoppingCartCustomer" }, method = RequestMethod.POST)
  public String shoppingCartCustomerSave(HttpServletRequest request, //
        Model model,
        @ModelAttribute("customerForm") @Validated CustomerForm customerForm,
        BindingResult result, 
        final RedirectAttributes redirectAttributes) {

     if (result.hasErrors()) {
        customerForm.setValid(false);
        return "shoppingCartCustomer";
     }

     customerForm.setValid(true);
     CartInfo cartInfo = Utils.getCartInSession(request);
     CustomerInfo customerInfo = new CustomerInfo(customerForm);
     cartInfo.setCustomerInfo(customerInfo);

     return "redirect:/shoppingCartConfirmation";
  }

  // GET: Confirmare informatii
  @RequestMapping(value = { "/shoppingCartConfirmation" }, method = RequestMethod.GET)
  public String shoppingCartConfirmationReview(HttpServletRequest request, Model model) {
     
	  CartInfo cartInfo = Utils.getCartInSession(request);

     if (cartInfo == null || cartInfo.isEmpty()) {

        return "redirect:/shoppingCart";
        
     } 
     else if (!cartInfo.isValidCustomer()) {

        return "redirect:/shoppingCartCustomer";
     }
     model.addAttribute("myCart", cartInfo);
     return "shoppingCartConfirmation";
  }

  // POST: Salvare cos
  @RequestMapping(value = { "/shoppingCartConfirmation" }, method = RequestMethod.POST)
  public String shoppingCartConfirmationSave(HttpServletRequest request, Model model) {
    
	  CartInfo cartInfo = Utils.getCartInSession(request);

     if (cartInfo.isEmpty()) {

        return "redirect:/shoppingCart";
        
     } 
     else if (!cartInfo.isValidCustomer()) 
     {
        return "redirect:/shoppingCartCustomer";
     }
     try {
        orderDAO.saveOrder(cartInfo);
     } catch (Exception e) {

        return "shoppingCartConfirmation";
     }

     Utils.removeCartInSession(request);
     Utils.storeLastOrderedCartInSession(request, cartInfo);

     return "redirect:/shoppingCartFinalize";
  }

  @RequestMapping(value = { "/shoppingCartFinalize" }, method = RequestMethod.GET)
  public String shoppingCartFinalize(HttpServletRequest request, Model model) {

     CartInfo lastOrderedCart = Utils.getLastOrderedCartInSession(request);

     if (lastOrderedCart == null) {
        return "redirect:/shoppingCart";
     }
     model.addAttribute("lastOrderedCart", lastOrderedCart);
     return "shoppingCartFinalize";
  }

  @RequestMapping(value = { "/productImage" }, method = RequestMethod.GET)
  public void productImage(HttpServletRequest request, HttpServletResponse response, Model model,
        @RequestParam("code") String code) throws IOException {
     Product product = null;
     if (code != null) {
        product = this.productDAO.findProduct(code);
     }
     if (product != null && product.getImage() != null) {
        response.setContentType("image/jpeg, image/jpg, image/png, image/gif");
        response.getOutputStream().write(product.getImage());
     }
     response.getOutputStream().close();
  }

}