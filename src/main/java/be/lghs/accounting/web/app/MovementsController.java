package be.lghs.accounting.web.app;

import be.lghs.accounting.configuration.Roles;
import be.lghs.accounting.model.tables.records.MovementCategoriesRecord;
import be.lghs.accounting.model.tables.records.MovementsRecord;
import be.lghs.accounting.repositories.AccountRepository;
import be.lghs.accounting.repositories.MovementRepository;
import be.lghs.accounting.repositories.SubscriptionRepository;
import be.lghs.accounting.services.MovementService;
import lombok.RequiredArgsConstructor;
import org.jooq.Result;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Controller
@RequestMapping("/app/movements")
@RequiredArgsConstructor
public class MovementsController {

    private final MovementRepository movementRepository;
    private final MovementService movementService;
    private final SubscriptionRepository subscriptionRepository;
    private final AccountRepository accountRepository;

    @GetMapping
    @Transactional(readOnly = true)
    @Secured(Roles.ROLE_ADMIN)
    public String movements(Model model) {
        var movements = movementRepository.findAll();
        var categories = movementRepository.categories();
        var categoryNamesById = movementRepository.categoryNamesById();

        model.addAttribute("movements", movements);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryNamesById", categoryNamesById);
        return "app/movements/list";
    }

    @GetMapping("/{movement_id}")
    @Transactional(readOnly = true)
    @Secured(Roles.ROLE_ADMIN)
    public String movements(@PathVariable("movement_id") UUID movementId,
                            Model model) {
        var movement = movementRepository.getOne(movementId);
        var categories = movementRepository.categories();

        model.addAttribute("movement", movement);
        model.addAttribute("categories", categories);
        return "app/movements/view";
    }

    @Transactional
    @PostMapping("/{movement_id}/category")
    @Secured(Roles.ROLE_TREASURER)
    public String setCategory(@PathVariable("movement_id") UUID movementId,
                              @RequestParam("category_id") UUID categoryId) {
        if (categoryId == null) {
            return "redirect:/app/movements";
        }
        movementRepository.setCategory(movementId, categoryId);
        return "redirect:/app/movements#"+movementId;
    }

    @Transactional(readOnly = true)
    @GetMapping("/by-iban/{iban}")
    @Secured(Roles.ROLE_TREASURER)
    public String movementsFromIban(@PathVariable("iban") String iban,
                                    Model model) {
        var movements = movementRepository.findFromCounterParty(iban);
        var categories = movementRepository.categories();
        var categoryNamesById = movementRepository.categoryNamesById();

        model.addAttribute("movements", movements);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryNamesById", categoryNamesById);
        return "app/movements/list";
    }

    @Transactional(readOnly = true)
    @GetMapping("/by-category/{category_id}")
    @Secured(Roles.ROLE_TREASURER)
    public String movementsFromIban(@PathVariable("category_id") UUID categoryId,
                                    Model model) {
        var movements = movementRepository.findByCategory(categoryId);
        var categories = movementRepository.categories();
        var categoryNamesById = movementRepository.categoryNamesById();

        model.addAttribute("movements", movements);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryNamesById", categoryNamesById);
        return "app/movements/list";
    }

    @Transactional(readOnly = true)
    @GetMapping("/{movement_id}/split")
    @Secured(Roles.ROLE_TREASURER)
    public String splitMovementForm(@PathVariable("movement_id") UUID movementId,
                                    Model model) {
        MovementsRecord movement = movementRepository.getOne(movementId);
        Result<MovementCategoriesRecord> categories;
        if (movement.getAmount().signum() > 0) {
            categories = movementRepository.credits();
        } else {
            categories = movementRepository.debits();
        }

        model.addAttribute("movement", movement);
        model.addAttribute("categories", categories);
        return "app/movements/split";
    }

    @Transactional
    @PostMapping("/{movement_id}/split")
    @Secured(Roles.ROLE_TREASURER)
    public String splitMovement(@PathVariable("movement_id") UUID movementId,
                                @RequestParam("communication") String communication,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam("category") UUID categoryId,
                                @RequestParam("communication_split") String communicationSplit,
                                @RequestParam("amount_split") BigDecimal amountSplit,
                                @RequestParam("category_split") UUID categorySplitId) {
        MovementsRecord partOne = movementService.splitMovement(
            movementId,
            communication, amount, categoryId,
            communicationSplit, amountSplit, categorySplitId);
        MovementsRecord movement = movementRepository.getOne(movementId);
        return "redirect:/app/movements#" + partOne.getId();
    }

    @Transactional(readOnly = true)
    @GetMapping("/{movement_id}/subscription")
    @Secured(Roles.ROLE_TREASURER)
    public String subscriptionForm(@PathVariable("movement_id") UUID movementId,
                                   Model model) {
        var subscription = subscriptionRepository.getForMovement(movementId);


        model.addAttribute("subscription", subscription);
        model.addAttribute("monthFormatter", DateTimeFormatter.ofPattern("YYYY-MM"));

        return "/app/subscriptions/form";
    }

    @Transactional(readOnly = true)
    @GetMapping("/add")
    @Secured(Roles.ROLE_ADMIN)
    public String movementForm(Model model) {
        var accounts = accountRepository.findAll();
        model.addAttribute("accounts", accounts);
        return "/app/movements/add";
    }

    @Transactional
    @PostMapping("/add")
    @Secured(Roles.ROLE_ADMIN)
    public String addMovement(@RequestParam("account_id") UUID accountId,
                              @RequestParam("amount") BigDecimal amount,
                              @RequestParam("communication") String communication,
                              @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        MovementsRecord movement = movementRepository.addMovement(
                accountId, amount, communication, date);
        return "redirect:/app/movements#" + movement.getId();
    }
}
