defmodule Backend.RoomChannel do
    use Phoenix.Channel

    def join("room:lobby", _message, socket) do
      {:ok, socket}
    end

    def get_module(module_string) do
      :erlang.binary_to_existing_atom(module_string <> <<>>, :utf8)
    end

    def handle_in("get_actors", attrs, socket) do
        actor_modules = Backend.CodeServer.getActorTypes()
        {:reply, {:ok, %{:actors => actor_modules}}, socket}
    end

    def handle_in("start_actor", %{"type" => name}, socket) do
        IO.puts "start actor received #{name}"
        case GenServerProxy.start_link(name) do
            {:ok, pid} -> 
              IO.inspect pid
              {:reply, {:ok, %{:name => name, :pid => to_string(:erlang.pid_to_list(pid))}}, socket}
            _ ->          {:reply, {:error, %{:reason => "Unable to start actor type: #{name}"}}, socket}
        end
    end

    def handle_in("new_actor", %{"name" => name}, socket) do
        IO.puts "new actor received #{name}"

        case File.read("code/template/actor.ex") do
          {:ok, body} ->
            {:ok, file} = File.open "code/#{name}.ex", [:write]
            IO.binwrite file, String.replace(body, "{{actor_name}}", name)
            Code.eval_file("code/#{name}.ex")
            {:reply, {:ok, %{:name => name}}, socket}
          {:error, reason} ->
            IO.inspect reason
            {:reply, {:error, %{:reason => reason}}, socket}
        end
    end

    def handle_in("update_actor", %{"name" => name, "actor_code" => code }, socket) do
      IO.puts "update actor received #{name}"

      {:ok, file} = File.open "code/#{name}.ex", [:write]
      IO.binwrite file, code
        try do
            IO.inspect Code.eval_file("code/#{name}.ex")
            {:reply, {:ok, %{:name => name}}, socket}
        rescue
            e ->
            IO.inspect e
            {:reply, {:error, %{:reson => e}}, socket}
        end
    end

    def handle_in("get_actor_code", %{"name" => name}, socket) do
        IO.puts "getting actor code #{name}"
        case File.read("code/#{name}.ex") do
          {:ok, body} ->
            {:reply, {:ok, %{:code => body}}, socket}
          {:error, reason} ->
            {:reply, {:error, %{:reason => reason}}, socket}
        end
    end

    def handle_in("send_msg", %{"name" => name, "to_pid" => pid, "msg" => msg}, socket) do
        IO.puts "send msg"
        pid = :erlang.list_to_pid(to_charlist(pid))
        rec = :"Elixir.#{name}".send_msg(pid, msg)
        {:reply, {:ok, %{:received => rec}}, socket}
    end

end
