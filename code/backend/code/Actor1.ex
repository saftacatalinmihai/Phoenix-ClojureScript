defmodule Actor1 do
    use GenServer
    require Logger

    def start_link do
       GenServer.start_link(__MODULE__, :ok, [])
    end

    def ping(server) do
        GenServer.call(server, "ping")
    end

    def send_msg(server, msg) do
      GenServer.call(server, msg)
    end

    def init(:ok) do
        {:ok, %{}}
    end

    def handle_call("ping", _from, state) do
        {:reply, "pong", state}
    end

    def handle_cast("ping", state) do
        {:noreply, state}
    end

    def code_change(old_vsn, old_state, _extra) do
        {:ok, old_state}
    end

end