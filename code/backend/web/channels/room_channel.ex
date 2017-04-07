defmodule Backend.RoomChannel do
    use Phoenix.Channel

    def join("room:lobby", _message, socket) do
      {:ok, socket}
    end

    def get_module(module_string) do
      :erlang.binary_to_existing_atom(module_string <> <<>>, :utf8)
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

    def handle_in("send_msg", %{"name" => _name, "to_pid" => pid, "msg" => msg}, socket) do
        IO.puts "send msg"
        pid = :erlang.list_to_pid(to_charlist(pid))
        rec = GenServer.call(pid, msg)
        {:reply, {:ok, %{:received => rec}}, socket}
    end

end
