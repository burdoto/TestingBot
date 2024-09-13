package com.ampznetwork.testbot;

import com.ampznetwork.chatmod.api.ChatMod;
import com.ampznetwork.chatmod.api.model.ChatMessage;
import com.ampznetwork.chatmod.core.formatting.ChatMessageFormatter;
import com.ampznetwork.libmod.api.LibMod;
import com.ampznetwork.libmod.api.entity.Player;
import com.ampznetwork.libmod.api.interop.game.IPlayerAdapter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.util.TriState;
import org.comroid.api.func.util.Command;
import org.comroid.api.func.util.Event;
import org.comroid.api.io.FileHandle;
import org.comroid.api.java.StackTraceUtils;
import org.comroid.api.text.StringMode;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.easymock.EasyMock.*;

public class Bot implements EventListener {
    private static final String FORMAT = "%message%";
    private static       Bot    INSTANCE;

    public static void main(String[] args) {
        INSTANCE = new Bot(new FileHandle(args[0]).getContent(true));
    }

    @Command
    public static TextComponent format(@Command.Arg(stringMode = StringMode.GREEDY) String string) {
        var sender    = Player.builder().id(UUID.randomUUID()).name("Steve").build();
        var formatter = ChatMessageFormatter.builder().format(FORMAT).build();

        IPlayerAdapter playerAdapter = mock(IPlayerAdapter.class);
        expect(playerAdapter.getPlayer(sender.getId())).andReturn(Optional.of(sender)).anyTimes();
        expect(playerAdapter.checkPermission(sender.getId(), "chatmod.format.bold")).andReturn(TriState.TRUE).anyTimes();
        expect(playerAdapter.checkPermission(sender.getId(), "chatmod.format.italic")).andReturn(TriState.TRUE).anyTimes();
        expect(playerAdapter.checkPermission(sender.getId(), "chatmod.format.underline")).andReturn(TriState.TRUE).anyTimes();
        expect(playerAdapter.checkPermission(sender.getId(), "chatmod.format.strikethrough")).andReturn(TriState.TRUE).anyTimes();
        expect(playerAdapter.checkPermission(sender.getId(), "chatmod.format.hidden_links")).andReturn(TriState.TRUE).anyTimes();
        LibMod lib = mock(LibMod.class);
        expect(lib.getPlayerAdapter()).andReturn(playerAdapter).anyTimes();
        ChatMod mod = mock(ChatMod.class);
        expect(mod.getLib()).andReturn(lib).anyTimes();
        expect(mod.applyPlaceholders(sender.getId(), FORMAT)).andReturn(FORMAT).anyTimes();

        replay(playerAdapter, lib, mod);

        var msg = new ChatMessage(sender, string, string, Component.text(string));
        formatter.accept(mod, msg);
        //var json = GsonComponentSerializer.gson().serialize(msg.getText());
        //return "```json\n"+json+"\n```";
        return msg.getText();
    }

    private final Event.Bus<GenericEvent> bus;
    private final Command.Manager         cmdr;
    private final JDA                     jda;

    public Bot(String token) {
        this.bus = new Event.Bus<>();
        bus.flatMap(MessageReceivedEvent.class).subscribeData(event -> {
            try {
                var raw = event.getMessage().getContentRaw();
                if (!raw.startsWith("!format"))
                    return;

                //var string    = "&cThis is red and &nthis is only underlined. **Google is at https://google.com**";
                var string = raw.substring("!format ".length());
                var result = format(string);
                var json   = GsonComponentSerializer.gson().serialize(result);
                event.getMessage().reply("```json\n" + json + "\n```").addFiles(FileUpload.fromData(Util.component2img(result), "text.png")).queue();
            } catch (Throwable t) {
                event.getMessage().reply("```\n" + StackTraceUtils.toString(t) + "\n```");
            }
        });

        this.jda = JDABuilder.createDefault(token).enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
        jda.addEventListener(this);

        this.cmdr = new Command.Manager();
        cmdr.new Adapter$JDA(jda);
        cmdr.register(Bot.class);
        cmdr.initialize();
    }

    @Override
    public void onEvent(GenericEvent genericEvent) {
        bus.publish(genericEvent);
    }
}
