package com.avairebot.commands.fun;

import com.avairebot.AvaIre;
import com.avairebot.chat.SimplePaginator;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.commands.CommandContext;
import com.avairebot.factories.RequestFactory;
import com.avairebot.language.I18n;
import com.avairebot.requests.Response;
import com.avairebot.requests.service.PunService;
import com.avairebot.utilities.NumberUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.helper.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class PunCommand extends Command
{
    private final String templateUrl = "https://icanhazdadjoke.com/";

    public PunCommand(AvaIre avaire)
    {
        super(avaire);
    }



    /**
     * Gets the command name, this is used in help and error
     * messages as the title as well as log messages.
     *
     * @return Never-null, the command name.
     */
    @Override
    public String getName() {
        return "Pun Command";
    }

    @Override
    public String getDescription() {
        return "Gets a random pun or a list of puns based on a search query.";
    }


    @Override
    public List<String> getUsageInstructions() {
        return Arrays.asList(
            "`:command` - Gets a random pun",
                "`:command <query> [page]` - retrieves a list of puns ");
    }

    @Override
    public List<String> getExampleUsage() {
        return Arrays.asList(
            "`:command` - Gets a random pun.",
            "`:command chicken 2` - Gets the second page of a search for chicken puns"
        );
    }

    /**
     * Gets am immutable list of command triggers that can be used to invoke the current
     * command, the first index in the list will be used when the `:command` placeholder
     * is used in {@link #getDescription(CommandContext)} or {@link #getUsageInstructions()} methods.
     *
     * @return An immutable list of command triggers that should invoked the command.
     */
    @Override
    public List<String> getTriggers() {
        return Arrays.asList("pun","dadjoke");
    }

    @Override
    public List<String> getMiddleware() {
        return Collections.singletonList("throttle:channel,2,5");
    }

    /**
     * The command executor, this method is invoked by the command handler
     * and the middleware stack when a user sends a message matching the
     * commands prefix and one of its command triggers.
     *
     * @param context The command message context generated using the
     *                JDA message event that invoked the command.
     * @param args    The arguments given to the command, if no arguments was given the array will just be empty.
     * @return true on success, false on failure.
     */
    @Override
    public boolean onCommand(CommandMessage context, String[] args)
    {
        if(args.length == 0)
        {

            RequestFactory.makeGET(templateUrl)
                .addHeader("Accept", "application/json")
                .send((Consumer<Response>) response ->
                {
                    JSONObject json = new JSONObject(response.toString());
                    sendPun(context, json);
                });
        }
        else
        {
            List<String> phrase = new ArrayList<>();
            if (StringUtil.isNumeric(args[args.length - 1]))
            {
                phrase.addAll(Arrays.asList(args).subList(0, args.length - 1));
            }
            else
            {
                phrase.addAll(Arrays.asList(args));
            }
            RequestFactory.makeGET(templateUrl + "/search")
                .addHeader("Accept", "application/json")
                .addParameter("term",String.join(" ", phrase))
                .send((Consumer<Response>) response ->
                {
                    PunService service = (PunService)response.toService(PunService.class);
                    if (!service.hasData()) {
                        context.makeWarning(context.i18n("noResults"))
                            .set("query", String.join(" ", args))
                            .queue();
                        return;
                    }

                    sendPunList(context,args, service.getResults());
                });
        }

        return true;
    }

    private void sendPun(CommandMessage context, JSONObject json)
    {
        context.makeSuccess(json.getString("joke"))
            .queue();
    }

    private boolean sendPunList(CommandMessage context, String[] args, List<PunService.Pun> resultList)
    {
        List<String> punsToSortThrough = new ArrayList<>();

        StringBuilder userPhraseWriter = new StringBuilder();

        if(StringUtil.isNumeric(args[args.length -1]))
        {
            userPhraseWriter.append(String.join("\n \n", Arrays.asList(args).subList(0, args.length - 1)));
        }
        else
        {
            userPhraseWriter.append(String.join("\n \n ", Arrays.asList(args)));
        }

        for (PunService.Pun pun : resultList)
        {
            punsToSortThrough.add(pun.getJoke() + "\n");
        }


        SimplePaginator<String> paginator = new SimplePaginator<>(punsToSortThrough, 10);
        if (args.length > 1)
        {
            paginator.setCurrentPage(NumberUtil.parseInt(args[args.length - 1], 1));

        }

        List<String> sortedPuns = new ArrayList<>();
        paginator.forEach((index, key, val) -> sortedPuns.add(val));

        context.makeInfo(":puns\n\n:paginator")
            .setTitle(context.i18n("title"))
            .set("puns", String.join("\n", sortedPuns))
            .set(
                "paginator", paginator.generateFooter(
                context.getGuild(),
                generateCommandTrigger(context.getMessage()) + " " + userPhraseWriter.toString())
            )
            .queue();

        return true;
    }
}
