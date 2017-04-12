defmodule Backend.RoomChannel do
    use Phoenix.Channel

    def join("room:lobby", _message, socket) do
      send(self(), :after_join)
      {:ok, socket}
    end

    def event_pusher(socket) do
      receive do
        ev -> 
          # IO.puts "<Ev>"
          # IO.inspect ev
          # IO.puts "<Ev/>"
          # TODO: fix to string for PIDS in events back to client
# ** (Protocol.UndefinedError) protocol String.Chars not implemented for #PID<0.612.0>
# backend_1   |     (elixir) lib/string/chars.ex:3: String.Chars.impl_for!/1
# backend_1   |     (elixir) lib/string/chars.ex:17: String.Chars.to_string/1
# backend_1   |     (backend) web/event_store.ex:38: anonymous fn/2 in Backend.EventStore.handle_info/2
# backend_1   |     (elixir) lib/enum.ex:645: Enum."-each/2-lists^foreach/1-0-"/2
# backend_1   |     (elixir) lib/enum.ex:645: Enum.each/2
# backend_1   |     (backend) web/event_store.ex:38: Backend.EventStore.handle_info/2
# backend_1   |     (stdlib) gen_server.erl:601: :gen_server.try_dispatch/4
# backend_1   |     (stdlib) gen_server.erl:667: :gen_server.handle_msg/5
# backend_1   |     (stdlib) proc_lib.erl:247: :proc_lib.init_p_do_apply/3
# backend_1   | Last message: {:trace_ts, #PID<0.613.0>, :receive, {:"$gen_call", {#PID<0.427.0>, #Reference<0.0.2.5615>}, "ping"}, {1491, 569762, 812365}}
          push socket, "event", %{:event => :ok}
          event_pusher(socket)
      end
    end

    def handle_info(:after_join, socket) do
      spawn( fn -> 
        Backend.EventStore.register_watcher(self())
        event_pusher(socket) 
      end)
      {:noreply, socket}
    end

    def handle_in("get_actors", _attrs, socket) do
        actor_modules = Backend.CodeServer.get_actor_types()
        {:reply, {:ok, %{:actors => actor_modules}}, socket}
    end

    def handle_in("start_actor", %{"type" => name}, socket) do
        IO.puts "start actor received #{name}"
        {:reply, Backend.CodeServer.start_actor(name), socket}
    end

    def handle_in("new_actor", %{"name" => name}, socket) do
        IO.puts "new actor received #{name}"
        {:reply, Backend.CodeServer.new_actor_type(name), socket}
    end

    def handle_in("update_actor", %{"name" => name, "actor_code" => code }, socket) do
      IO.puts "update actor received #{name}"
      {:reply, Backend.CodeServer.update_actor_code(name, code), socket}
    end

    def handle_in("get_actor_code", %{"name" => name}, socket) do
      IO.puts "getting actor code #{name}"
      {:reply, Backend.CodeServer.get_actor_code(name), socket}
    end

    def handle_in("send_msg", %{"to" => pid, "msg" => msg}, socket) do
      IO.puts "send msg"
      IO.inspect pid, msg
      {:reply, Backend.CodeServer.send_msg(pid, msg), socket}
    end

end
